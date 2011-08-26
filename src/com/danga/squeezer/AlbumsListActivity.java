
package com.danga.squeezer;

import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.danga.squeezer.itemlists.IServiceAlbumListCallback;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.service.AlbumCache;
import com.danga.squeezer.service.AlbumCache.Albums;
import com.danga.squeezer.service.AlbumCacheCursor;
import com.danga.squeezer.service.AlbumCacheProvider;
import com.danga.squeezer.service.ISqueezeService;
import com.danga.squeezer.service.SqueezeService;
import com.danga.squeezer.service.SqueezerServerState;

public class AlbumsListActivity extends ListActivity implements AbsListView.OnScrollListener,
        AbsListView.OnItemClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = AlbumsListActivity.class.getName();
    private static Bundle LiveUpdateT = new Bundle();
    private static Bundle LiveUpdateF = new Bundle();

    private ListView mListView = null;

    private ContentProviderClient mContentProviderClient;
    private AlbumCacheProvider mProvider;
    private Cursor mCursor;

    /** The number of items per page. */
    private final int mPageSize = Squeezer.getContext().getResources()
            .getInteger(R.integer.PageSize);

    // Columns to bind to resources (in order)
    private static final String[] from = new String[] {
            Albums.COL_NAME, Albums.COL_ARTIST, Albums.COL_ARTWORK_PATH
    };

    // Resources to bind column values to (in order)
    private static final int[] to = new int[] {
            R.id.text1, R.id.text2, R.id.icon
    };

    private static final int PROGRESS_DIALOG = 0;
    private ProgressDialog progressDialog;

    private ISqueezeService mService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = ISqueezeService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albums_list_activity);

        mListView = getListView();
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);

        resetListAdapter();

        LiveUpdateT.putInt("TYPE", AlbumCacheCursor.TYPE_LIVEUPDATE);
        LiveUpdateT.putBoolean("LiveUpdate", true);
        LiveUpdateF.putInt("TYPE", AlbumCacheCursor.TYPE_LIVEUPDATE);
        LiveUpdateF.putBoolean("LiveUpdate", false);

    }

    /**
     *
     */
    private void resetListAdapter() {
        mCursor = managedQuery(Albums.CONTENT_URI,
                new String[] {
                        Albums._ID, Albums.COL_NAME, Albums.COL_ARTIST, Albums.COL_ARTWORK_PATH
                },
                null, null, null);

        setTitle("Albums: " + mCursor.getCount());

        SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this,
                R.layout.albums_list_entry, mCursor, from, to);

        setListAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mContentProviderClient =
                getContentResolver().acquireContentProviderClient(Albums.CONTENT_URI);
        mProvider = (AlbumCacheProvider) mContentProviderClient.getLocalContentProvider();

        Intent intent = new Intent(this, SqueezeService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mContentProviderClient != null)
            mContentProviderClient.release();

        try {
            mService.unregisterAlbumListCallback(albumListCallback);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (serviceConnection != null)
            unbindService(serviceConnection);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.albumslist, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_cache:
                if (mProvider != null) {
                    mCursor.close();
                    mProvider.rebuildCache();
                    resetListAdapter();
                }

                return true;

                /**
                 * Progress dialog approach. Set up a callback to catch new
                 * albums when they arrive. The callback posts messages to a
                 * handler. The handler updates the state of the progress dialog
                 * on each message, and requests the next set of albums from the
                 * server.
                 */
            case R.id.preload_cache:
                if (mProvider != null) {
                    try {
                        mService.registerAlbumListCallback(albumListCallback);
                        mCursor.close();
                        showDialog(PROGRESS_DIALOG);
                        mService.albums(0, "album", null, null, null, null);

                        /*
                         * The other half of this (resetting the list adapter,
                         * turning off the dialog box, etc) is in
                         * mProgressHandler.
                         */
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return true;
        }

        return false;
    }

    /**
     * A handler for messages posted by albumListCallback.
     */
    final Handler mProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int total = msg.arg1;
            progressDialog.setProgress(total);
            if (total >= progressDialog.getMax()) {
                dismissDialog(PROGRESS_DIALOG);
                try {
                    mService.unregisterAlbumListCallback(albumListCallback);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                resetListAdapter();
            } else if (total != 0) { // Skip the very first result received.
                try {
                    Log.v(TAG, "Calling albums(" + msg.arg2 + ")");
                    mService.albums(msg.arg2, "album", null, null, null, null);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };

    private final IServiceAlbumListCallback albumListCallback = new IServiceAlbumListCallback.Stub() {
        /**
         * Update the affected rows.
         *
         * @param count Number of items as reported by squeezeserver.
         * @param start The start position of items in this update.
         * @param items New items to update in the cache
         */
        public void onAlbumsReceived(int count, int start, List<SqueezerAlbum> items)
                throws RemoteException {
            int page = start / mPageSize;
            Message msg = mProgressHandler.obtainMessage();
            msg.arg1 = start;
            msg.arg2 = (page + 1) * mPageSize; // The next page of albums to
                                               // fetch
            mProgressHandler.sendMessage(msg);
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
        }
    };

    /** Dialog methods */

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("Loading...");
                return progressDialog;
            default:
                return null;
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case PROGRESS_DIALOG:
                progressDialog.setProgress(0);
                try {
                    progressDialog.setMax(mService.getServerState().getTotalAlbums());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
        }
    }

    /** OnItemClickListener methods */

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Uri albumUri = ContentUris
                .withAppendedId(AlbumCache.Albums.CONTENT_ID_URI_BASE, id);
        Intent i = new Intent(Intent.ACTION_VIEW, albumUri);
        startActivity(i);
    }

    /** OnScrollListener methods */

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                mCursor.respond(LiveUpdateT);

                Bundle extras = new Bundle();
                extras.putInt("TYPE", AlbumCacheCursor.TYPE_REQUEST_PAGE);
                extras.putInt("firstPosition", view.getFirstVisiblePosition());
                extras.putInt("lastPosition", view.getLastVisiblePosition());
                mCursor.respond(extras);

                break;

            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                break;

            case OnScrollListener.SCROLL_STATE_FLING:
                mCursor.respond(LiveUpdateF);
        }
    }
}
