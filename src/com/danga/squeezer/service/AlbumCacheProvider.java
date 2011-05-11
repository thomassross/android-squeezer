package com.danga.squeezer.service;

import java.util.Iterator;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.danga.squeezer.model.SqueezerAlbum;

public class AlbumCacheProvider extends ContentProvider {
	private static final String TAG = "AlbumCacheProvider";
	private static final String DATABASE_NAME = "album_cache.db";
	private static final int DATABASE_VERSION = 1;
	
    /*
     * Constants used by the Uri matcher to choose an action based on the pattern
     * of the incoming URI
     */
    // The incoming URI matches the Albums URI pattern
    private static final int ALBUMS = 1;

    // The incoming URI matches the Album ID URI pattern
    private static final int ALBUM_ID = 2;

    // The incoming URI matches the Live Folder URI pattern
    private static final int LIVE_FOLDER_ALBUMS = 3;

    /**
     * A UriMatcher instance
     */
    private static final UriMatcher sUriMatcher;
	
	private DatabaseHelper mOpenHelper;
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
        // Add a pattern that routes URIs terminated with "albums" to an ALBUMS operation
        sUriMatcher.addURI(AlbumCache.AUTHORITY, "albums", ALBUMS);

        // Add a pattern that routes URIs terminated with "albums" plus an integer
        // to an album ID operation
        sUriMatcher.addURI(AlbumCache.AUTHORITY, "albums/#", ALBUM_ID);

        // Add a pattern that routes URIs terminated with live_folders/albums to a
        // live folder operation
        sUriMatcher.addURI(AlbumCache.AUTHORITY, "live_folders/albums", LIVE_FOLDER_ALBUMS);
		
	}
	
	static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + AlbumCache.Albums.TABLE_NAME + " ("
					+ AlbumCache.Albums.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AlbumCache.Albums.COL_SERVERORDER + " INTEGER,"
	                + AlbumCache.Albums.COL_ALBUMID + " TEXT,"
	                + AlbumCache.Albums.COL_NAME + " TEXT,"
	                + AlbumCache.Albums.COL_ARTIST + " TEXT,"
	                + AlbumCache.Albums.COL_YEAR + " TEXT,"
	                + AlbumCache.Albums.COL_ARTWORK + " TEXT"
	                + ");");		
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + AlbumCache.Albums.TABLE_NAME + ";");
			onCreate(db);
		}
	}
	
	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
        // Validates the incoming URI. Only the full provider URI is allowed for inserts.
        if (sUriMatcher.match(uri) != ALBUMS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // A map to hold the new record's values.
        ContentValues values;

        // If the incoming values map is not null, uses it for the new values.
        if (initialValues != null) {
            values = new ContentValues(initialValues);

        } else {
            // Otherwise, create a new value map
            values = new ContentValues();
        }

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Performs the insert and returns the ID of the new note.
        long rowId = db.insert(AlbumCache.Albums.TABLE_NAME, null, values);

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID appended to it.
            Uri albumUri = ContentUris.withAppendedId(AlbumCache.Albums.CONTENT_ID_URI_BASE, rowId);

            // Notifies observers registered against this provider that the data changed.
            getContext().getContentResolver().notifyChange(albumUri, null);
            return albumUri;
        }

        // If the insert didn't succeed, then the rowID is <= 0. Throws an exception.
        throw new SQLException("Failed to insert row into " + uri);
    }
	
	// TODO: Might need to keep this, and call via
	// getContentResolver().acquireContentProviderClient(uri) if the speed up is
	// worthwhile.
	public int batchUpdateViaServerOrder(Uri uri, int startServerOrder, List<SqueezerAlbum> albums) {
		if (sUriMatcher.match(uri) != ALBUMS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
    	SQLiteDatabase db = mOpenHelper.getWritableDatabase();

    	ContentValues cv = new ContentValues();
    	int serverOrder = startServerOrder;
    	SqueezerAlbum thisAlbum;
    	
    	db.beginTransaction();
    	try {
    		Iterator<SqueezerAlbum> it = albums.iterator();
    		while (it.hasNext()) {
    			thisAlbum = it.next();
    			cv.put(AlbumCache.Albums.COL_NAME, thisAlbum.getName());
    	    	cv.put(AlbumCache.Albums.COL_ALBUMID, thisAlbum.getId());
    	       	cv.put(AlbumCache.Albums.COL_ARTIST, thisAlbum.getArtist());
    	    	cv.put(AlbumCache.Albums.COL_YEAR, thisAlbum.getYear());
    	    	cv.put(AlbumCache.Albums.COL_ARTWORK, thisAlbum.getArtwork_track_id());
    	    	
    	    	db.update(AlbumCache.Albums.TABLE_NAME, cv,
    	    			AlbumCache.Albums.COL_SERVERORDER + "=?",
    	    			new String[] {Integer.toString(serverOrder)});
    	    	
    	    	serverOrder++;
			}
    		
    		db.setTransactionSuccessful();
    	} finally {
    		db.endTransaction();
    	}
    	
        getContext().getContentResolver().notifyChange(uri, null);
    	
    	return serverOrder - startServerOrder;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // Does the update based on the incoming URI pattern
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI matches the general albums pattern, does the update based on
            // the incoming data.
            case ALBUMS:

                // Does the update and returns the number of rows updated.
                count = db.update(
                    AlbumCache.Albums.TABLE_NAME, // The database table name.
                    values,                   // A map of column names and new values to use.
                    where,                    // The where clause column names.
                    whereArgs                 // The where clause column values to select on.
                );
                break;

            // If the incoming URI matches a single albums ID, does the update based on the incoming
            // data, but modifies the where clause to restrict it to the particular album ID.
            case ALBUM_ID:
                /*
                 * Starts creating the final WHERE clause by restricting it to the incoming
                 * note ID.
                 */
                finalWhere =
                        AlbumCache.Albums._ID +                          // The ID column name
                        " = " +                                          // test for equality
                        uri.getPathSegments().                           // the incoming note ID
                            get(AlbumCache.Albums.ALBUM_ID_PATH_POSITION);

                // If there were additional selection criteria, append them to the final WHERE
                // clause
                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // Does the update and returns the number of rows updated.
                count = db.update(
                    AlbumCache.Albums.TABLE_NAME, // The database table name.
                    values,                       // A map of column names and new values to use.
                    finalWhere,                   // The final WHERE clause to use
                                                  // placeholders for whereArgs
                    whereArgs                     // The where clause column values to select on, or
                                                  // null if the values are in the where argument.
                );
                break;
            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /* Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows updated.
        return count;	}

}
