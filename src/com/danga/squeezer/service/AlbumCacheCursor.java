package com.danga.squeezer.service;

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
	private int mColIndex = 0;
	
	public AlbumCacheCursor(Cursor cursor, AlbumCacheProvider provider) {
		super(cursor);
		
		mCursor = cursor;
		mProvider= provider;
		
		mColIndex = super.getColumnIndex(AlbumCache.Albums.COL_ALBUMID);
	}

	@Override
	public String getString(int columnIndex) {
		// TODO: Should be mColIndex, not the hardcoded constant here, but the 
		// call to getColumnIndex() in the constructor returns -1 for unknown
		// reasons.
		String albumId = mCursor.getString(2);
		if (albumId.equals("") && mLiveUpdate) {
			int position = getPosition();
			requestPageAtPosition(position);
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
