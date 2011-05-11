package com.danga.squeezer;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import com.danga.squeezer.service.AlbumCache.Albums;

public class AlbumListActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.album_list_activity);
		
		Cursor cursor = getContentResolver().query(Albums.CONTENT_URI,
				new String[] { Albums._ID, Albums.COL_NAME, Albums.COL_ARTIST },
				null, null, null);
		
		// Columns to bind
		String[] from= new String[] { Albums.COL_NAME, Albums.COL_ARTIST };
		int[] to = new int[] { R.id.album_name, R.id.album_artist };
		
		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this,
				R.layout.album_list_entry, cursor, from, to);
		
		this.setListAdapter(mAdapter);
	}
	
	static void show(Context context) {
        final Intent intent = new Intent(context, AlbumListActivity.class);
        context.startActivity(intent);
    }
}
