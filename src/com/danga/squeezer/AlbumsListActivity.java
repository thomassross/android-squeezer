
package com.danga.squeezer;

import android.app.ListActivity;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.danga.squeezer.service.AlbumCache;
import com.danga.squeezer.service.AlbumCache.Albums;
import com.danga.squeezer.service.AlbumCacheCursor;

public class AlbumsListActivity extends ListActivity implements AbsListView.OnScrollListener,
        AbsListView.OnItemClickListener {
    private static final String TAG = AlbumsListActivity.class.getName();
    private static Bundle LiveUpdateT = new Bundle();
    private static Bundle LiveUpdateF = new Bundle();

    private ListView mListView = null;

    private ContentProviderClient mContentProviderClient = null;
    private Cursor cursor = null;

    // Columns to bind to resources (in order)
    private static final String[] from = new String[] {
            Albums.COL_NAME, Albums.COL_ARTIST, Albums.COL_ARTWORK_PATH
    };

    // Resources to bind column values to (in order)
    private static final int[] to = new int[] {
            R.id.text1, R.id.text2, R.id.icon
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albums_list_activity);

        mListView = getListView();
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);

        cursor = managedQuery(Albums.CONTENT_URI,
                new String[] {
                        Albums._ID, Albums.COL_NAME, Albums.COL_ARTIST, Albums.COL_ARTWORK_PATH
                },
                null, null, null);

        setTitle("Albums: " + cursor.getCount());

        SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this,
                R.layout.albums_list_entry, cursor, from, to);

        LiveUpdateT.putInt("TYPE", AlbumCacheCursor.TYPE_LIVEUPDATE);
        LiveUpdateT.putBoolean("LiveUpdate", true);
        LiveUpdateF.putInt("TYPE", AlbumCacheCursor.TYPE_LIVEUPDATE);
        LiveUpdateF.putBoolean("LiveUpdate", false);

        setListAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mContentProviderClient =
                getContentResolver().acquireContentProviderClient(Albums.CONTENT_URI);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mContentProviderClient != null)
            mContentProviderClient.release();
    }

    static void show(Context context) {
        final Intent intent = new Intent(context, AlbumsListActivity.class);
        context.startActivity(intent);
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
                cursor.respond(LiveUpdateT);

                Bundle extras = new Bundle();
                extras.putInt("TYPE", AlbumCacheCursor.TYPE_REQUEST_PAGE);
                extras.putInt("firstPosition", view.getFirstVisiblePosition());
                extras.putInt("lastPosition", view.getLastVisiblePosition());
                cursor.respond(extras);

                break;

            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                break;

            case OnScrollListener.SCROLL_STATE_FLING:
                cursor.respond(LiveUpdateF);
        }
    }

}
