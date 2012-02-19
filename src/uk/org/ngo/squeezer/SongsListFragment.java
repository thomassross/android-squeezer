
package uk.org.ngo.squeezer;

import uk.org.ngo.squeezer.Squeezer;

import uk.org.ngo.squeezer.service.SongCache;
import uk.org.ngo.squeezer.service.SongCacheCursor;
import uk.org.ngo.squeezer.service.SongCache.Songs;
import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;


/**
 * List fragment that shows songs from the content provider.
 * <p>
 * Sends messages to the cursor indicating whether or not the cursor should
 * perform live updates of the data, based on whether or not the list is
 * scrolling.
 * <p>
 * List item clicks are reported to the hosting activity, which must implement
 * the OnSongSelectedListener interface.
 * 
 * @author nik
 */
public class SongsListFragment extends ListFragment implements AbsListView.OnScrollListener,
        LoaderCallbacks<Cursor> {
    @SuppressWarnings("unused")
    private static final String TAG = SongsListFragment.class.getName();
    private static Bundle LiveUpdateT = new Bundle();
    private static Bundle LiveUpdateF = new Bundle();

    /** The number of items per page. */
    private final int mPageSize = Squeezer.getContext().getResources()
            .getInteger(R.integer.PageSize);

    /** The active cursor, so we can send it messages. */
    private Cursor mCursor;

    /** Total number of rows (songs) in the dataset. */
    private int mRowCount;

    SimpleCursorAdapter mAdapter;
    private OnSongSelectedListener mListener;

    /** Columns to fetch from the database. */
    private static final String[] boundColumns = new String[] {
            Songs._ID, Songs.COL_NAME, Songs.COL_ARTIST, Songs.COL_ARTWORK_PATH
    };

    /** Columns to bind to resources (in order). */
    private static final String[] from = new String[] {
            Songs.COL_NAME, Songs.COL_ARTIST, Songs.COL_ARTWORK_PATH
    };

    /** Resources to bind column values to (in order). */
    private static final int[] to = new int[] {
            R.id.text1, R.id.text2, R.id.icon
    };

    /**
     * Interface for responding to clicks on list items.
     * <p>
     * Activities that host this fragment must implement this interface.
     */
    public interface OnSongSelectedListener {
        /**
         * The user has selected an song.
         * 
         * @param songUri The selected song.
         */
        public void onSongSelected(Uri songUri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnScrollListener(this);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.songs_list_entry, null, from, to, 0);

        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);

        LiveUpdateT.putInt("TYPE", SongCacheCursor.TYPE_LIVEUPDATE);
        LiveUpdateT.putBoolean("LiveUpdate", true);
        LiveUpdateF.putInt("TYPE", SongCacheCursor.TYPE_LIVEUPDATE);
        LiveUpdateF.putBoolean("LiveUpdate", false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSongSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSongSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Uri songUri = ContentUris
                .withAppendedId(SongCache.Songs.CONTENT_ID_URI_BASE, id);
        mListener.onSongSelected(songUri);
    }

    /** LoaderCallbacks */

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Songs.CONTENT_URI, boundColumns,
                null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mCursor = data;
        mRowCount = mCursor.getCount();
        getActivity().setTitle("Songs: " + data.getCount());
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
        mAdapter.swapCursor(null);
    }

    /** OnScrollListener methods */

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    /**
     * Handle the list being scrolled. If it is now idle then signal the cursor
     * to perform live updates, and request the page(s) of data necessary to
     * show the first and last item. If is being flung then disable live
     * updates.
     */
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mCursor == null)
            return;

        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                mCursor.respond(LiveUpdateT);

                Bundle idle = new Bundle();
                idle.putInt("TYPE", SongCacheCursor.TYPE_REQUEST_PAGE);
                idle.putInt("firstPosition", view.getFirstVisiblePosition());
                idle.putInt("lastPosition", view.getLastVisiblePosition());
                mCursor.respond(idle);

                break;

            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                break;

            case OnScrollListener.SCROLL_STATE_FLING:
                mCursor.respond(LiveUpdateF);

                /**
                 * Per http://code.google.com/p/android/issues/detail?id=15182,
                 * we might not receive the SCROLL_STATE_IDLE message if the
                 * user has flung down to the very bottom of the list.
                 * <p>
                 * If this is the last page of data then fetch it irrespective
                 * of the scroll state.
                 */
                int lastPosition = view.getLastVisiblePosition();
                if (lastPosition + mPageSize > mRowCount) {
                    Bundle fling = new Bundle();
                    fling.putInt("TYPE", SongCacheCursor.TYPE_REQUEST_PAGE);
                    fling.putInt("firstPosition", view.getFirstVisiblePosition());
                    fling.putInt("lastPosition", lastPosition);
                    mCursor.respond(fling);
                }

                break;
        }
    }
}
