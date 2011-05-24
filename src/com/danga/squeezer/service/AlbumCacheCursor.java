package com.danga.squeezer.service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;

import com.danga.squeezer.R;
import com.danga.squeezer.Squeezer;
import com.google.android.panoramio.BitmapUtils;

public class AlbumCacheCursor extends CursorWrapper {
	private static final String TAG = AlbumCacheCursor.class.getName();

	public static final int TYPE_LIVEUPDATE = 0;
	public static final int TYPE_REQUEST_PAGE = 1;
	
	private Cursor mCursor;
	private AlbumCacheProvider mProvider;
	
	/**
	 * Default album artwork
	 */
	private static byte[] mDefaultArtwork;
	
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
		defaults.put(AlbumCache.Albums.COL_NAME, "Loading... (name)");
		defaults.put(AlbumCache.Albums.COL_ARTIST, "Loading... (artist)");
		defaults.put(AlbumCache.Albums.COL_YEAR, "Loading... (year)");
		defaults.put(AlbumCache.Albums.COL_ARTWORK_ID, "Loading... (artwork)");
	}
	
	public AlbumCacheCursor(Cursor cursor, AlbumCacheProvider provider) {
		super(cursor);
		
		mCursor = cursor;
		mProvider= provider;
	}
	
	@Override
	public String getString(int columnIndex) {
		// TODO: Assumes there's at least two columns, and ID is always first
		
		String testVal = mCursor.getString(1);
		if (testVal == null) {
			if (mLiveUpdate) {
				int position = getPosition();
				requestPageAtPosition(position);
			}

			String thisColumnName = getColumnName(columnIndex);
			
			if (thisColumnName == AlbumCache.Albums.COL_ID)
				return super.getString(columnIndex);
				
			return defaults.get(thisColumnName);			
		}
			
		return super.getString(columnIndex);
	}
	
	@Override
	public byte[] getBlob(int columnIndex) {
		byte[] img = super.getBlob(columnIndex);
		
		if (img == null) {
			if (mDefaultArtwork == null) {
				Log.v(TAG, "Creating default album artwork");
				// Approach 1
//				ByteArrayOutputStream out = new ByteArrayOutputStream();			
//				Bitmap b = BitmapFactory.decodeResource(Squeezer.getContext().getResources(), R.drawable.icon_album_noart);
//				b.compress(Bitmap.CompressFormat.JPEG, 100, out);
//				ByteArrayOutputStream out = new ByteArrayOutputStream();
//				BitmapUtils.copyInputStreamToOutputStream(in, out);

				
				// Approach 2
				InputStream in = Squeezer.getContext().getResources().openRawResource(R.drawable.icon_album_noart);
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            try {
					BitmapUtils.copyInputStreamToOutputStream(in, (OutputStream) out);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
				
				
				mDefaultArtwork = out.toByteArray(); //out.toByteArray();
			}
			
			return mDefaultArtwork;
		}
		
		return img;
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