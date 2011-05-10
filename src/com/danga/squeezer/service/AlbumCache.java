package com.danga.squeezer.service;

import android.provider.BaseColumns;

/**
 * Represent the album cache database.
 *
 */
public final class AlbumCache {
	private AlbumCache() {}

	public static final class Db implements BaseColumns {
		private Db() {}
		
		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "album_cache";
		
		public static final String TABLE_NAME = "album";
		
		public static final String COL_ID = android.provider.BaseColumns._ID;
		public static final String COL_SERVERORDER = "serverorder";
		public static final String COL_ALBUMID = "albumid";
		public static final String COL_NAME = "name";
		public static final String COL_ARTIST = "artist";
		public static final String COL_YEAR = "year";
		public static final String COL_ARTWORK = "artwork";
	}
}
