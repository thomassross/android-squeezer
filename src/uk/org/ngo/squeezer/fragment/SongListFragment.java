package uk.org.ngo.squeezer.fragment;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.OrderPages;
import uk.org.ngo.squeezer.framework.SqueezerItemAdapter;
import uk.org.ngo.squeezer.framework.SqueezerItemListAdapter;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.itemlists.IServiceSongListCallback;
import uk.org.ngo.squeezer.itemlists.SqueezerSongView;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerSongOrderDialog.SongsSortOrder;
import uk.org.ngo.squeezer.model.QueryParameters;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.util.ImageFetcher;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;


/**
 * List all the songs that meet a particular criteria.
 *
 * For example, all songs by a particular artist, or on a particular album.
 */
public class SongListFragment extends ListFragment implements OrderPages {
    private static final String TAG = SongListFragment.class.getSimpleName();

    /** Progress bar (spinning) while items are loading. */
    private ProgressBar mLoadingProgress;
    private SqueezerItemView<SqueezerSong> mItemView;
    private OnSongSelectedListener mListener;

    private final Set<Integer> orderedPages = new HashSet<Integer>();

    /** The list is being scrolled by the user. */
    public boolean mListScrolling;

    /** The activity hosting this fragment. */
    private Activity mActivity;

    private QueryParameters mQueryParameters;

    private SqueezerItemAdapter<SqueezerSong> itemAdapter;

    private ImageFetcher mImageFetcher;

    private SongsSortOrder sortOrder = SongsSortOrder.title;

    private ISqueezeService mService = null;

    private final Handler uiThreadHandler = new UiThreadHandler(this);

    private ListView mListView;

    // TODO: Maybe the fragment should use getActivity() and get the
    // handler from the containing activity?
    private final static class UiThreadHandler extends Handler {
        WeakReference<Fragment> mFragment;

        public UiThreadHandler(Fragment fragment) {
            mFragment = new WeakReference<Fragment>(fragment);
        }
    };

    /**
     * Use this to post Runnables to work off thread
     */
    public Handler getUIThreadHandler() {
        return uiThreadHandler;
    }


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(TAG, "ServiceConnection.onServiceConnected()");
            mService = ISqueezeService.Stub.asInterface(binder);
            try {
                SongListFragment.this.onServiceConnected();
            } catch (RemoteException e) {
                Log.e(TAG, "Error in onServiceConnected: " + e);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        };
    };

    /**
     * Interface for responding to clicks on songs.
     * <p>
     * Activities that host this fragment must implement this interface.
     */
    public interface OnSongSelectedListener {
        /**
         * The user has selected a song.
         */
        public void onSongSelected(SqueezerSong song);
    }
    
    /**
     * Interface for requesting query parameters.
     * <p>
     * Activities that host this fragment must implement this interface.
     */
    public interface GetQueryParameters {
        /**
         * Return the query parameters.
         */
        public QueryParameters getQueryParameters();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        mActivity = activity;

        try {
            mListener = (OnSongSelectedListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSongSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity.bindService(new Intent(mActivity, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; serviceStub = " + mService);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListView = getListView();

        registerForContextMenu(mListView);

        mListView.setOnScrollListener(new ScrollListener());
        mListView.setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
                if (imageView != null) {
                    imageView.setImageBitmap(null);
                }
            }
        });

        mListView.setOnScrollListener(new ScrollListener());
        mQueryParameters = ((GetQueryParameters) mActivity).getQueryParameters();
    }

    /**
     * Creates the context menu, deferring to the {@link SqueezerItemAdapter.onCreateContextMenu} to
     * determine what the context menu should look like for these items.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getItemAdapter().onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     * Handles context menu selections by passing them to {@link SqueezerItemAdapter.doItemContext}.
     */
    @Override
    public final boolean onContextItemSelected(MenuItem menuItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) menuItem.getMenuInfo();

        if (mService != null) {
            try {
                return getItemAdapter().doItemContext(menuItem, menuInfo.position);
            } catch (RemoteException e) {
                Log.e(getTag(), "Error context menu action '" + menuInfo + "' for '"
                        + menuInfo.position + "': " + e);
            }
        }

        return super.onContextItemSelected(menuItem);
    }

    protected void onServiceConnected() throws RemoteException {
        registerCallback();
        orderItems();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceConnection != null) {
            try {
                unregisterCallback();
            } catch (RemoteException e) {
            }
            mActivity.unbindService(serviceConnection);
        }
    }

    protected void registerCallback() throws RemoteException {
        mService.registerSongListCallback(songListCallback);
    }

    protected void unregisterCallback() throws RemoteException {
        mService.unregisterSongListCallback(songListCallback);
    }

    private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
        public void onSongsReceived(int count, int start, List<SqueezerSong> items) {
            onItemsReceived(count, start, items);
        }
    };


    /**
     * Generic handler for new items from the server.
     * <p>
     * Updates the list adapter with the items that have been received.
     * 
     * @param count How many items to add.
     * @param start Where in the list to add them.
     * @param items The items to add.
     */
    public void onItemsReceived(final int count, final int start, final List<SqueezerSong> items) {
        getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                getItemAdapter().update(count, start, items);
            }
        });
    }

    /**
     * @return The current {@link SqueezerItemAdapter}, creating it if necessary.
     */
    public SqueezerItemAdapter<SqueezerSong> getItemAdapter() {
        return (SqueezerItemAdapter<SqueezerSong>) (itemAdapter == null ? (itemAdapter = createItemListAdapter(getItemView()))
                : itemAdapter);
    }

    protected SqueezerItemAdapter<SqueezerSong> createItemListAdapter(
            SqueezerItemView<SqueezerSong> itemView) {
        return new SqueezerItemListAdapter<SqueezerSong>(this, itemView, mImageFetcher);
    }

    /**
     * @return The current {@link SqueezerItemView}, creating it if necessary
     */
    public SqueezerItemView<SqueezerSong> getItemView() {
        return mItemView == null ? (mItemView = createItemView()) : mItemView;
    }

    public SqueezerItemView<SqueezerSong> createItemView() {
        return new SqueezerSongView(this);
    }

    /**
     * Order items from the start, and prepare an adapter to receive them
     * 
     * @throws RemoteException
     */
    public void orderItems() {
        reorderItems();
        // mListView.setVisibility(View.GONE);
        // loadingProgress.setVisibility(View.VISIBLE);
        clearItemListAdapter();
    }

    /**
     * Set the adapter to handle the display of the items, see also
     * {@link #setListAdapter(android.widget.ListAdapter)}
     * 
     * @param listAdapter
     */
    private void clearItemListAdapter() {
        setListAdapter(getItemAdapter());
    }

    /**
     * Order page at specified position, if it has not already been ordered.
     * 
     * @param pagePosition
     */
    public void maybeOrderPage(final int pagePosition) {
        if (!mListScrolling && !orderedPages.contains(pagePosition)) {
            orderedPages.add(pagePosition);
            try {
                orderPage(pagePosition);
            } catch (final RemoteException e) {
                Log.e(getTag(), "Error ordering items (" + pagePosition + "): " + e);
                // XXX: Maybe call orderedPages.remove(pagePosition) here, since the
                // page is not found.
            }
        }
    }

    public void orderPage(int start) throws RemoteException {
        mService.songs(start, sortOrder.name(), mQueryParameters.searchString,
                mQueryParameters.album, mQueryParameters.artist, mQueryParameters.year,
                mQueryParameters.genre);
    }

    /**
     * Clear all information about which pages has been ordered, and reorder the first page
     */
    public void reorderItems() {
        orderedPages.clear();
        // XXX: Maybe call getItemAdapter().clear() here?
        maybeOrderPage(0);
    }

    /**
     * Tracks scrolling activity.
     * <p>
     * When the list is idle, new pages of data are fetched from the server.
     * <p>
     * Use a TouchListener to work around an Android bug where SCROLL_STATE_IDLE messages are not
     * delivered after SCROLL_STATE_TOUCH_SCROLL messages. *
     */
    protected class ScrollListener implements AbsListView.OnScrollListener {
        private TouchListener mTouchListener = null;
        private final int mPageSize = getResources().getInteger(R.integer.PageSize);
        private boolean mAttachedTouchListener = false;

        private int mPrevScrollState = OnScrollListener.SCROLL_STATE_IDLE;

        /**
         * Sets up the TouchListener.
         * <p>
         * Subclasses must call this.
         */
        public ScrollListener() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
                mTouchListener = new TouchListener(this);
            }
        }

        // Deliberately left empty -- it is not called when the scroll completes, it appears to be
        // called multiple time during a scroll, including during flinging.
        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
                final int totalItemCount) {
        }

        @Override
        public void onScrollStateChanged(final AbsListView listView, final int scrollState) {
            if (scrollState == mPrevScrollState) {
                return;
            }

            if (mAttachedTouchListener == false) {
                if (mTouchListener != null) {
                    listView.setOnTouchListener(mTouchListener);
                }
                mAttachedTouchListener = true;
            }

            switch (scrollState) {
                case OnScrollListener.SCROLL_STATE_IDLE:
                    mListScrolling = false;

                    int pos = (listView.getFirstVisiblePosition() / mPageSize) * mPageSize;
                    final int end = listView.getFirstVisiblePosition() + listView.getChildCount();

                    while (pos < end) {
                        maybeOrderPage(pos);
                        pos += mPageSize;
                    }

                    break;

                case OnScrollListener.SCROLL_STATE_FLING:
                case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                    mListScrolling = true;
                    break;
            }

            mPrevScrollState = scrollState;
        }

        /**
         * Work around a bug in (at least) API levels 7 and 8.
         * <p>
         * The bug manifests itself like so: after completing a TOUCH_SCROLL the
         * system does not deliver a SCROLL_STATE_IDLE message to any attached
         * listeners.
         * <p>
         * In addition, if the user does TOUCH_SCROLL, IDLE, TOUCH_SCROLL you
         * would expect to receive three messages. You don't -- you get the
         * first TOUCH_SCROLL, no IDLE message, and then the second touch
         * doesn't generate a second TOUCH_SCROLL message.
         * <p>
         * This state clears when the user flings the list.
         * <p>
         * The simplest work around for this app is to track the user's finger,
         * and if the previous state was TOUCH_SCROLL then pretend that they
         * finished with a FLING and an IDLE event was triggered. This serves to
         * unstick the message pipeline.
         */
        protected class TouchListener implements View.OnTouchListener {
            private final OnScrollListener mOnScrollListener;

            public TouchListener(final OnScrollListener onScrollListener) {
                mOnScrollListener = onScrollListener;
            }

            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                final int action = event.getAction();
                final boolean mFingerUp = action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL;
                if (mFingerUp && mPrevScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    Log.v(TAG, "Sending special scroll state bump");
                    mOnScrollListener.onScrollStateChanged((AbsListView) view,
                            OnScrollListener.SCROLL_STATE_FLING);
                    mOnScrollListener.onScrollStateChanged((AbsListView) view,
                            OnScrollListener.SCROLL_STATE_IDLE);
                }
                return false;
            }
        }
    }
}
