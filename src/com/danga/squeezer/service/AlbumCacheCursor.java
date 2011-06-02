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
		// TODO: Localise
		defaults.put(AlbumCache.Albums.COL_NAME, "Loading...");
		defaults.put(AlbumCache.Albums.COL_ARTWORK_PATH, "android.resource://com.danga.squeezer/" + R.drawable.icon_album_noart);
	}
	
	public AlbumCacheCursor(Cursor cursor, AlbumCacheProvider provider) {
		super(cursor);
		
		mCursor = cursor;
		mProvider= provider;
	}
	
	@Override
	public String getString(int columnIndex) {
		String theString = mCursor.getString(columnIndex);
		
		if (theString == null) {
			String thisColumnName = getColumnName(columnIndex);

			// Name?  If so, kick off a fetch.
			if (thisColumnName.equals(AlbumCache.Albums.COL_NAME)) {
				String testVal = mCursor.getString(columnIndex);
				if (testVal == null) {
					if (mLiveUpdate) {
						requestPageAtPosition(getPosition());
					}
				}
				
				return defaults.get(AlbumCache.Albums.COL_NAME);
			}
			
			// Got a default value for it?  If so, return it.
			if (defaults.containsKey(thisColumnName)) {
				return defaults.get(thisColumnName);
			}
			
			// Everything else?  Return the null.
			return null;
		}
		
		return theString;
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