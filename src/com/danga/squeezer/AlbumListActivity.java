package com.danga.squeezer;

import android.app.ListActivity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.SimpleCursorAdapter;

import com.danga.squeezer.service.AlbumCache.Albums;
import com.danga.squeezer.service.AlbumCacheCursor;

public class AlbumListActivity extends ListActivity implements AbsListView.OnScrollListener {
	private static final String TAG = AlbumListActivity.class.getName();
	private static Bundle LiveUpdateT = new Bundle();
	private static Bundle LiveUpdateF = new Bundle();
	
	private ContentProviderClient mContentProviderClient = null;
	private Cursor cursor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.album_list_activity);
		
		cursor = managedQuery(Albums.CONTENT_URI,
				new String[] { Albums._ID, Albums.COL_NAME, Albums.COL_ARTIST, Albums.COL_ARTWORK_PATH },
				null, null, null);

		// Columns to bind
		String[] from = new String[] { Albums.COL_NAME, Albums.COL_ARTIST, Albums.COL_ARTWORK_PATH };
		int[] to = new int[] { R.id.text1, R.id.text2, R.id.icon };
		
		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this,
				R.layout.album_list_entry, cursor, from, to);

		LiveUpdateT.putInt("TYPE", AlbumCacheCursor.TYPE_LIVEUPDATE);
		LiveUpdateT.putBoolean("LiveUpdate", true);
		LiveUpdateF.putInt("TYPE", AlbumCacheCursor.TYPE_LIVEUPDATE);
		LiveUpdateF.putBoolean("LiveUpdate", false);

		setListAdapter(mAdapter);
	
		getListView().setOnScrollListener(this);
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
		if (mContentProviderClient != null) {
			mContentProviderClient.release();
		}
	}
	
	static void show(Context context) {
        final Intent intent = new Intent(context, AlbumListActivity.class);
        context.startActivity(intent);
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
            extras.putInt("position", view.getFirstVisiblePosition());
            cursor.respond(extras);

            break;
        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
            break;
        case OnScrollListener.SCROLL_STATE_FLING:
        	Log.v(TAG, "Flinging");
            cursor.respond(LiveUpdateF);
        }
    }
	
}
