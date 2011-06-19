
package com.danga.squeezer.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import com.danga.squeezer.Preferences;
import com.danga.squeezer.R;
import com.danga.squeezer.Squeezer;
import com.danga.squeezer.itemlists.IServiceAlbumListCallback;
import com.danga.squeezer.model.SqueezerAlbum;
import com.google.android.panoramio.BitmapUtils;

public class AlbumCacheProvider extends ContentProvider {
    private static final String TAG = "AlbumCacheProvider";

    /**
     * The resource to use when no album artwork exists.
     */
    private static final String defaultAlbumArtUri = Integer.toString(R.drawable.icon_album_noart);

    private SqueezerServerState mServerState;

    private ISqueezeService service;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
            try {
                service.registerAlbumListCallback(albumListCallback);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        public void onServiceDisconnected(ComponentName name) {
            service = null;
        };
    };

    /**
     * Batch database update notifications. Maintain a boolean that indicates
     * whether or not the content resolver should be notified about a database
     * change, and a pool of threads to do this notification.
     */
    private final AtomicBoolean notifyUpdates = new AtomicBoolean(false);
    private final ScheduledExecutorService notifyUpdatesThreadPool = Executors
            .newScheduledThreadPool(1);

    /** The set of pages that have been fetched from the server so far. */
    private final Set<Integer> mOrderedPages = new HashSet<Integer>();

    /** The number of items per page. */
    private final int mPageSize = Squeezer.getContext().getResources()
            .getInteger(R.integer.PageSize);

    /**
     * Thread pool for background image fetches.
     */
    private final ExecutorService artworkThreadPool = Executors.newFixedThreadPool(1);

    /**
     * @return The squeezeservice, or null if not bound
     */
    public ISqueezeService getService() {
        return service;
    }

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sAlbumsProjectionMap;

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sLiveFolderProjectionMap;

    /*
     * Constants used by the Uri matcher to choose an action based on the
     * pattern of the incoming URI
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

    /**
     * A representation of the directory that the provider uses as the root of
     * the cache.
     */
    private File mCacheDirectory;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a pattern that routes URIs terminated with "albums" to an ALBUMS
        // operation
        sUriMatcher.addURI(AlbumCache.AUTHORITY, "albums", ALBUMS);

        // Add a pattern that routes URIs terminated with "albums" plus an
        // integer
        // to an album ID operation
        sUriMatcher.addURI(AlbumCache.AUTHORITY, "albums/#", ALBUM_ID);

        // Add a pattern that routes URIs terminated with live_folders/albums to
        // a
        // live folder operation
        sUriMatcher.addURI(AlbumCache.AUTHORITY, "live_folders/albums", LIVE_FOLDER_ALBUMS);

        /*
         * Creates and initializes a projection map that returns all columns
         */

        // Creates a new projection map instance. The map returns a column name
        // given a string. The two are usually equal.
        sAlbumsProjectionMap = new HashMap<String, String>();

        sAlbumsProjectionMap.put(AlbumCache.Albums.COL_SERVERORDER,
                AlbumCache.Albums.COL_SERVERORDER);
        sAlbumsProjectionMap.put(AlbumCache.Albums.COL_ALBUMID, AlbumCache.Albums.COL_ALBUMID);
        sAlbumsProjectionMap.put(AlbumCache.Albums.COL_NAME, AlbumCache.Albums.COL_NAME);
        sAlbumsProjectionMap.put(AlbumCache.Albums.COL_ARTIST, AlbumCache.Albums.COL_ARTIST);
        sAlbumsProjectionMap.put(AlbumCache.Albums.COL_YEAR, AlbumCache.Albums.COL_YEAR);
        sAlbumsProjectionMap
                .put(AlbumCache.Albums.COL_ARTWORK_ID, AlbumCache.Albums.COL_ARTWORK_ID);
        sAlbumsProjectionMap.put(AlbumCache.Albums.COL_ARTWORK_PATH,
                AlbumCache.Albums.COL_ARTWORK_PATH);
        sAlbumsProjectionMap.put("_data", "_data");

        /*
         * Creates an initializes a projection map for handling Live Folders
         */

        // Creates a new projection map instance
        sLiveFolderProjectionMap = new HashMap<String, String>();

        // Maps "_ID" to "_ID AS _ID" for a live folder
        sLiveFolderProjectionMap.put(LiveFolders._ID, AlbumCache.Albums._ID + " AS "
                + LiveFolders._ID);

        // Maps "NAME" to "title AS NAME"
        sLiveFolderProjectionMap.put(LiveFolders.NAME, AlbumCache.Albums.COL_NAME + " AS " +
                LiveFolders.NAME);
    }

    class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME_SUFFIX = "-album_cache.db";
        private static final int DATABASE_VERSION = 1;

        private final SqueezerServerState mServerState;
        private final SharedPreferences preferences;

        DatabaseHelper(Context context, SqueezerServerState serverState) {
            super(context, serverState.getUuid() + DATABASE_NAME_SUFFIX, null, DATABASE_VERSION);
            mServerState = serverState;

            preferences = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + AlbumCache.Albums.TABLE_NAME + " ("
                    + AlbumCache.Albums.COL_SERVERORDER + " INTEGER PRIMARY KEY,"
                    + AlbumCache.Albums.COL_ALBUMID + " TEXT,"
                    + AlbumCache.Albums.COL_NAME + " TEXT,"
                    + AlbumCache.Albums.COL_ARTIST + " TEXT,"
                    + AlbumCache.Albums.COL_YEAR + " TEXT,"
                    + AlbumCache.Albums.COL_ARTWORK_ID + " TEXT,"
                    + AlbumCache.Albums.COL_ARTWORK_PATH + " TEXT,"
                    + "_data TEXT" // Album artwork location metadata
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + AlbumCache.Albums.TABLE_NAME + ";");
            onCreate(db);
        }

        /*
         * (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#getWritableDatabase()
         */
        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
            SQLiteDatabase db = super.getWritableDatabase();

            Log.v(TAG, "getWriteableDatabase()");

            maybeRebuildCache(db, false);

            return db;
        }

        /**
         * @param db
         * @param force Always rebuild if this is true.
         */
        public void maybeRebuildCache(SQLiteDatabase db, boolean force) {
            final String uuid = mServerState.getUuid();

            if (uuid == null)
                throw new IllegalStateException(
                        "maybeRebuildCache() called when server has no uuid");

            // Check the cache is still valid, re-create if necessary
            final String lastScanKey = uuid + ":lastscan";
            final int oldLastScan = preferences.getInt(lastScanKey, 0);
            final int curLastScan = mServerState.getLastScan();

            /**
             * Recreate the database if we haven't got any record of the last
             * scan time, or we do, and they don't match.
             */
            if (force || oldLastScan == 0 || oldLastScan != curLastScan) {
                Log.v(TAG, "Rebuilding album cache... " + oldLastScan + " : " + curLastScan);
                InsertHelper ih = new InsertHelper(db, AlbumCache.Albums.TABLE_NAME);
                final int serverOrderColumn = ih.getColumnIndex(AlbumCache.Albums.COL_SERVERORDER);
                final int totalAlbums = mServerState.getTotalAlbums();

                onUpgrade(db, 0, 1);

                db.beginTransaction();
                try {
                    for (int i = 0; i < totalAlbums; i++) {
                        if (i % 50 == 0)
                            Log.v("reset", "Created " + i);
                        ih.prepareForInsert();
                        ih.bind(serverOrderColumn, i);
                        ih.execute();
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                getContext().getContentResolver().notifyChange(AlbumCache.Albums.CONTENT_URI, null);

                // Remove the cache directory
                if (mCacheDirectory.exists())
                    for (File f : mCacheDirectory.listFiles())
                        f.delete();

                mCacheDirectory.mkdirs();

                Editor editor = preferences.edit();
                editor.putInt(lastScanKey, curLastScan);
                editor.commit();
                Log.v(TAG, "... album cache rebuilt");
            }
        }
    }

    /**
     * Every two seconds check to see if the content resolver should be notified
     * about updates, and if it should, send the notification.
     */
    public AlbumCacheProvider() {
        notifyUpdatesThreadPool.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (notifyUpdates.getAndSet(false) == true)
                    getContext().getContentResolver().notifyChange(AlbumCache.Albums.CONTENT_URI,
                            null);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "AlbumCacheProvider::onCreate running");
        Context context = getContext();

        Intent intent = new Intent(context, SqueezeService.class);
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public AlbumCacheCursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs,
            String sortOrder) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(AlbumCache.Albums.TABLE_NAME);

        /**
         * Choose the projection and adjust the "where" clause based on URI
         * pattern-matching.
         */
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI is for albums, chooses the Albums projection
            case ALBUMS:
                qb.setProjectionMap(sAlbumsProjectionMap);
                break;

            /*
             * If the incoming URI is for a single album identified by its ID,
             * chooses the album ID projection, and appends "_ID = <albumID>" to
             * the where clause, so that it selects that single album.
             */
            case ALBUM_ID:
                qb.setProjectionMap(sAlbumsProjectionMap);
                qb.appendWhere(
                        AlbumCache.Albums._ID + // the name of the ID column
                                "=" +
                                // the position of the album ID itself in the
                                // incoming URI
                                uri.getPathSegments().get(AlbumCache.Albums.ALBUM_ID_PATH_POSITION));
                break;

            case LIVE_FOLDER_ALBUMS:
                // If the incoming URI is from a live folder, chooses the live
                // folder projection.
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;

            default:
                // If the URI doesn't match any of the known patterns, throw an
                // exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy;
        // If no sort order is specified, uses the default
        if (TextUtils.isEmpty(sortOrder))
            orderBy = AlbumCache.Albums.DEFAULT_SORT_ORDER;
        else
            // otherwise, uses the incoming sort order
            orderBy = sortOrder;

        // Opens the database object in "read" mode, since no writes need to be
        // done.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        /*
         * Performs the query. If no problems occur trying to read the database,
         * then a Cursor object is returned; otherwise, the cursor variable
         * contains null. If no records were selected, then the Cursor object is
         * empty, and Cursor.getCount() returns 0.
         */
        Cursor c = qb.query(
                db, // The database to query
                projection, // The columns to return from the query
                selection, // The columns for the where clause
                selectionArgs, // The values for the where clause
                null, // don't group the rows
                null, // don't filter by row groups
                orderBy // The sort order
                );

        // Tells the Cursor what URI to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return new AlbumCacheCursor(c, this);
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#getType(Uri)}. Returns the MIME
     * data type of the URI given as a parameter.
     *
     * @param uri The URI whose MIME type is desired.
     * @return The MIME type of the URI.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public String getType(Uri uri) {
        /**
         * Chooses the MIME type based on the incoming URI pattern
         */
        switch (sUriMatcher.match(uri)) {

            // If the pattern is for albums or live folders, returns the general
            // content type.
            case ALBUMS:
            case LIVE_FOLDER_ALBUMS:
                return AlbumCache.Albums.CONTENT_TYPE;

                // If the pattern is for album IDs, returns the album ID content
                // type.
            case ALBUM_ID:
                return AlbumCache.Albums.CONTENT_ITEM_TYPE;

                // If the URI pattern doesn't match any permitted patterns,
                // throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validates the incoming URI. Only the full provider URI is allowed for
        // inserts.
        if (sUriMatcher.match(uri) != ALBUMS)
            throw new IllegalArgumentException("Unknown URI " + uri);

        // A map to hold the new record's values.
        ContentValues values;

        // If the incoming values map is not null, uses it for the new values.
        if (initialValues != null)
            values = new ContentValues(initialValues);
        else
            // Otherwise, create a new value map
            values = new ContentValues();

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Performs the insert and returns the ID of the new note.
        long rowId = db.insert(AlbumCache.Albums.TABLE_NAME, null, values);

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID
            // appended to it.
            Uri albumUri = ContentUris.withAppendedId(AlbumCache.Albums.CONTENT_ID_URI_BASE, rowId);

            // Notifies observers registered against this provider that the data
            // changed.
            getContext().getContentResolver().notifyChange(albumUri, null);
            return albumUri;
        }

        // If the insert didn't succeed, then the rowID is <= 0. Throws an
        // exception.
        throw new SQLException("Failed to insert row into " + uri);
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
            // If the incoming URI matches the general albums pattern, does the
            // update based on
            // the incoming data.
            case ALBUMS:

                // Does the update and returns the number of rows updated.
                count = db.update(
                        AlbumCache.Albums.TABLE_NAME, // The database table
                                                      // name.
                        values, // A map of column names and new values to use.
                        where, // The where clause column names.
                        whereArgs // The where clause column values to select
                                  // on.
                        );
                break;

            // If the incoming URI matches a single albums ID, does the update
            // based on the incoming data, but modifies the where clause to
            // restrict it to the particular album ID.
            case ALBUM_ID:
                /*
                 * Starts creating the final WHERE clause by restricting it to
                 * the incoming note ID.
                 */
                finalWhere =
                        AlbumCache.Albums._ID + // The ID column name
                                " = " + // test for equality
                                uri.getPathSegments(). // the incoming note ID
                                        get(AlbumCache.Albums.ALBUM_ID_PATH_POSITION);

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (where != null)
                    finalWhere = finalWhere + " AND " + where;

                // Does the update and returns the number of rows updated.
                count = db.update(
                        AlbumCache.Albums.TABLE_NAME, // The database table
                                                      // name.
                        values, // A map of column names and new values to use.
                        finalWhere, // The final WHERE clause to use
                                    // placeholders for whereArgs
                        whereArgs // The where clause column values to select
                                  // on, or null if the values are in the where
                                  // argument.
                        );
                break;
            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*
         * Gets a handle to the content resolver object for the current context,
         * and notifies it that the incoming URI changed. The object passes this
         * along to the resolver framework, and observers that have registered
         * themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows updated.
        return count;
    }

    private final IServiceAlbumListCallback albumListCallback = new IServiceAlbumListCallback.Stub() {
        /**
         * Update the affected rows.
         *
         * @param count Number of items as reported by squeezeserver.
         * @param start The start position of items in this update.
         * @param items New items to update in the cache
         */
        public void onAlbumsReceived(int count, int start, List<SqueezerAlbum> items)
                throws RemoteException {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            ContentValues cv = new ContentValues();
            int serverOrder = start;
            SqueezerAlbum thisAlbum;
            String albumArtUrl = null;

            db.beginTransaction();
            try {
                Iterator<SqueezerAlbum> it = items.iterator();
                while (it.hasNext()) {
                    thisAlbum = it.next();
                    cv.put(AlbumCache.Albums.COL_NAME, thisAlbum.getName());
                    cv.put(AlbumCache.Albums.COL_ALBUMID, thisAlbum.getId());
                    cv.put(AlbumCache.Albums.COL_ARTIST, thisAlbum.getArtist());
                    cv.put(AlbumCache.Albums.COL_YEAR, thisAlbum.getYear());
                    cv.put(AlbumCache.Albums.COL_ARTWORK_ID, thisAlbum.getArtwork_track_id());

                    // Kick off a fetch of album artwork.
                    albumArtUrl = getAlbumArtUrl(thisAlbum.getArtwork_track_id());
                    if (albumArtUrl == null || albumArtUrl.length() == 0)
                        cv.put(AlbumCache.Albums.COL_ARTWORK_PATH, defaultAlbumArtUri);
                    else
                        updateAlbumArt(serverOrder, thisAlbum.getArtwork_track_id(), albumArtUrl);

                    db.update(AlbumCache.Albums.TABLE_NAME, cv,
                            AlbumCache.Albums.COL_SERVERORDER + "=?",
                            new String[] {
                                Integer.toString(serverOrder)
                            });

                    serverOrder++;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(AlbumCache.Albums.CONTENT_URI, null);
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
            Log.v(TAG, "onServerStateChanged");
            mServerState = newState;
            String uuid = mServerState.getUuid();

            Log.v(TAG, "AlbumCacheProvider: Server UUID is now " + uuid);

            if (uuid != null)
                mCacheDirectory = new File(Environment.getExternalStorageDirectory(),
                        "/Android/data/com.danga.squeezer/cache/album/"
                                + mServerState.getUuid());
            else
                mCacheDirectory = null;

            mOpenHelper = new DatabaseHelper(getContext(), mServerState);
        }
    };

    protected void updateAlbumArt(final int serverOrder, final String trackId,
            final String albumArtUrl) {

        artworkThreadPool.execute(new Runnable() {
            public void run() {
                if (!mCacheDirectory.exists())
                    if (!mCacheDirectory.mkdirs())
                        return;

                File artwork = new File(mCacheDirectory, trackId + ".jpg");
                File artwork64 = new File(mCacheDirectory, trackId + "-64px.png");

                // File artwork = new File(root, trackId + "/original.png");
                // File artwork64 = new File(root, trackId +
                // "/scaled-64px.png");

                try {
                    if (artwork.createNewFile()) {
                        InputStream in = null;
                        BufferedOutputStream out = null;
                        BufferedOutputStream out64 = null;

                        // Read the remote image, copy to disk
                        in = new BufferedInputStream(new URL(albumArtUrl).openStream(),
                                64 * 1024);
                        out = new BufferedOutputStream(new FileOutputStream(artwork), 64 * 1024);

                        BitmapUtils.copyInputStreamToOutputStream(in, out);
                        in.close();
                        out.close();

                        // Parse the image to obtain the dimensions
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inPurgeable = true;
                        opts.inInputShareable = true;
                        opts.inJustDecodeBounds = true;

                        Bitmap b = BitmapFactory.decodeFile(artwork.getAbsolutePath(), opts);

                        // Use the dimensions to compute a scaling factor
                        opts.inSampleSize = Math.max(opts.outHeight / 64, opts.outWidth / 64);
                        opts.inJustDecodeBounds = false;

                        // Parse the full image, scale to icon size, and
                        // save to a file
                        out64 = new BufferedOutputStream(new FileOutputStream(artwork64),
                                8 * 1024);
                        b = BitmapFactory.decodeFile(artwork.getAbsolutePath(), opts);

                        Bitmap b64 = Bitmap.createScaledBitmap(b, 64, 64, true);
                        b64.compress(Bitmap.CompressFormat.PNG, 100, out64);

                        out64.close();

                        // File is on disk, update the DB to point to it
                        ContentValues cv = new ContentValues();
                        cv.put(AlbumCache.Albums.COL_ARTWORK_PATH,
                                AlbumCache.Albums.CONTENT_ID_URI_BASE
                                        + Integer.toString(serverOrder));
                        cv.put("_data", artwork64.getAbsolutePath());

                        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

                        db.update(AlbumCache.Albums.TABLE_NAME, cv,
                                AlbumCache.Albums.COL_SERVERORDER + "=?",
                                new String[] {
                                    Integer.toString(serverOrder)
                                });

                        // Database content changed, a notification should
                        // be sent 'soon'
                        notifyUpdates.set(true);
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    private String getAlbumArtUrl(String artwork_track_id) {
        if (artwork_track_id == null)
            return null;

        if (service == null)
            return null;

        try {
            return service.getAlbumArtUrl(artwork_track_id);
        } catch (RemoteException e) {
            Log.e(TAG, "Error requesting album art url: " + e);
            return null;
        }
    }

    /**
	 *
	 */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        if (sUriMatcher.match(uri) != ALBUM_ID)
            throw new IllegalArgumentException("openFile not supported for multiple albums");

        return openFileHelper(uri, mode);
    }

    /**
     * Possibly fetch the requested page of data from the service, if it's not
     * previously been requested.
     *
     * @param page The index of the page to fetch.
     */
    public void maybeRequestPage(int page) {
        if (!mOrderedPages.contains(page))
            try {
                mOrderedPages.add(page);
                service.albums(page * mPageSize, "album", null, null, null, null);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                mOrderedPages.remove(page);
                e.printStackTrace();
            }
    }

    /**
     * Rebuild the provider's cache of information, irrespective of whether or
     * not it is out of date.
     */
    public void rebuildCache() {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        mOpenHelper.maybeRebuildCache(db, true);
        mOrderedPages.clear();
    }
}
