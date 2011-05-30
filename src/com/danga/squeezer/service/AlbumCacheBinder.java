package com.danga.squeezer.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

import com.danga.squeezer.R;
import com.danga.squeezer.Squeezer;
import com.google.android.panoramio.BitmapUtils;

public class AlbumCacheBinder implements SimpleCursorAdapter.ViewBinder {
	private static final String TAG = AlbumCacheBinder.class.getName();

	/**
	 * Default album artwork
	 */
	private static Bitmap mDefaultArtwork;
	
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if (view instanceof ImageView) {
			ImageView iv = (ImageView) view;
			String path = cursor.getString(columnIndex);
			
			if (path == null) {
				if (mDefaultArtwork == null) {
					Log.v(TAG, "Creating default album artwork");
					
					InputStream in = Squeezer.getContext().getResources().openRawResource(R.drawable.icon_album_noart);
		            ByteArrayOutputStream out = new ByteArrayOutputStream();
		            
		            try {
						BitmapUtils.copyInputStreamToOutputStream(in, (OutputStream) out);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
					
					byte[] img = out.toByteArray();
					mDefaultArtwork = BitmapFactory.decodeByteArray(img, 0, img.length);
				}
			
				iv.setImageBitmap(mDefaultArtwork);
				return true;
			}

			InputStream f;
			try {
				f = Squeezer.getContext().getContentResolver().openInputStream(Uri.parse(path));
				iv.setImageBitmap(BitmapFactory.decodeStream(f));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		}
		
		return false;
	}
}
