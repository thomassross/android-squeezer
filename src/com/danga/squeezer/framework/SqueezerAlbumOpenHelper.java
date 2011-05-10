package com.danga.squeezer.framework;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.danga.squeezer.service.AlbumCache;

/**
 * Helper for the Album cache database. 
 *
 */
public class SqueezerAlbumOpenHelper extends SQLiteOpenHelper {
	
	public SqueezerAlbumOpenHelper(Context context) {
		super(context, AlbumCache.Db.DATABASE_NAME, null, AlbumCache.Db.DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + AlbumCache.Db.TABLE_NAME + " ("
                + AlbumCache.Db.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + AlbumCache.Db.COL_SERVERORDER + " INTEGER,"
                + AlbumCache.Db.COL_ALBUMID + " TEXT,"
                + AlbumCache.Db.COL_NAME + " TEXT,"
                + AlbumCache.Db.COL_ARTIST + " TEXT,"
                + AlbumCache.Db.COL_YEAR + " TEXT,"
                + AlbumCache.Db.COL_ARTWORK + " TEXT"
                + ");");		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + AlbumCache.Db.TABLE_NAME + ";");
		onCreate(db);
	}
}
