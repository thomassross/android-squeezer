package com.danga.squeezer.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.os.RemoteException;

import com.danga.squeezer.R;
import com.danga.squeezer.Squeezer;

public class AlbumCacheCursor extends CursorWrapper {
	private static final String TAG = AlbumCacheCursor.class.getName();

	public static final int TYPE_LIVEUPDATE = 0;
	public static final int TYPE_REQUEST_PAGE = 1;
	
	private Cursor mCursor;
	private AlbumCacheProvider mProvider;
	
	/**
	 * Whether queries should fetch from the server for missing data.
	 */
	private Boolean mLiveUpdate = true;	

	private Set<Integer> mOrderedPages = new HashSet<Integer>();

    private int mPageSize = Squeezer.getContext().getResources().getInteger(R.integer.PageSize);
	
	/**
	 * Map column names to default values for those columns.
	 */
	private static HashMap<String, String> defaults = new HashMap<String, String>();
	{
		defaults.put(AlbumCache.Albums.COL_NAME, "Loading... (name)");
		defaults.put(AlbumCache.Albums.COL_ARTIST, "Loading... (artist)");
		defaults.put(AlbumCache.Albums.COL_YEAR, "Loading... (year)");
		defaults.put(AlbumCache.Albums.COL_ARTWORK, "Loading... (artwork)");
	}
	
	public AlbumCacheCursor(Cursor cursor, AlbumCacheProvider provider) {
		super(cursor);
		
		mCursor = cursor;
		mProvider= provider;
	}
	
	@Override
	public String getString(int columnIndex) {
		// TODO: Assumes there's two columns, and ID is always first
		
		String testVal = mCursor.getString(1);
		if (testVal == null && mLiveUpdate) {
			int position = getPosition();
			requestPageAtPosition(position);
			
			// TODO: Zeroth column is always ID, autoincrements, so has valid
			// data which can be returned immediately
			if (columnIndex == 0)
				return super.getString(columnIndex);
			
			String thisColumn = getColumnName(columnIndex);
			return defaults.get(thisColumn);			
		}
			
		return super.getString(columnIndex);
	}

	/**
	 * @param service
	 * @param position
	 */
	private void requestPageAtPosition(int position) {
		ISqueezeService service = mProvider.getService();
		int page = position / mPageSize;
		
		if (! mOrderedPages.contains(page)) {
			try {
				mOrderedPages.add(page);
				service.albums(page * mPageSize, "album", null, null, null, null);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public Bundle respond(Bundle extras) {
		int type = extras.getInt("TYPE");
		switch (type) {
		case TYPE_LIVEUPDATE:
			mLiveUpdate = extras.getBoolean("LiveUpdate");
			break;
		case TYPE_REQUEST_PAGE:
			int position = extras.getInt("position");
			requestPageAtPosition(position);
			break;
		default:
			throw new IllegalArgumentException("Unknown value in TYPE field of Bundle: " + type);
		}

		return Bundle.EMPTY;
	}	
}
