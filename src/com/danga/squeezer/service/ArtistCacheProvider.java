
package com.danga.squeezer.service;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import com.danga.squeezer.Preferences;
import com.danga.squeezer.R;
import com.danga.squeezer.Squeezer;
import com.danga.squeezer.itemlists.IServiceArtistListCallback;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.service.ArtistCache.Artists;

public class ArtistCacheProvider extends ContentProvider {
    private static final String TAG = "ArtistCacheProvider";

    private SqueezerServerState mServerState;

    private ISqueezeService service;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
            try {
                service.registerArtistListCallback(artistListCallback);
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

    /** The set of pages that have been ordered from the server. */
    private final Set<Integer> mOrderedPages = new HashSet<Integer>();

    /** The set of artwork that has been ordered from the server. */
    private final Set<Integer> mOrderedArtwork = new HashSet<Integer>();

    /** The number of items per page. */
    private final int mPageSize = Squeezer.getContext().getResources()
            .getInteger(R.integer.PageSize);

    /**
     * @return The squeezeservice, or null if not bound
     */
    public ISqueezeService getService() {
        return service;
    }

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sArtistsProjectionMap;

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sLiveFolderProjectionMap;

    /*
     * Constants used by the Uri matcher to choose an action based on the
     * pattern of the incoming URI
     */
    // The incoming URI matches the Artists URI pattern
    private static final int ARTISTS = 1;

    // The incoming URI matches the Artist ID URI pattern
    private static final int ARTIST_ID = 2;

    // The incoming URI matches the Live Folder URI pattern
    private static final int LIVE_FOLDER_ARTISTS = 3;

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

        // Add a pattern that routes URIs terminated with "artists" to an
        // ARTISTS
        // operation
        sUriMatcher.addURI(ArtistCache.AUTHORITY, "artists", ARTISTS);

        // Add a pattern that routes URIs terminated with "artists" plus an
        // integer
        // to an artist ID operation
        sUriMatcher.addURI(ArtistCache.AUTHORITY, "artists/#", ARTIST_ID);

        // Add a pattern that routes URIs terminated with live_folders/artists
        // to
        // a
        // live folder operation
        sUriMatcher.addURI(ArtistCache.AUTHORITY, "live_folders/artists", LIVE_FOLDER_ARTISTS);

        /*
         * Creates and initializes a projection map that returns all columns
         */

        // Creates a new projection map instance. The map returns a column name
        // given a string. The two are usually equal.
        sArtistsProjectionMap = new HashMap<String, String>();

        sArtistsProjectionMap.put(ArtistCache.Artists.COL_SERVERORDER,
                ArtistCache.Artists.COL_SERVERORDER);
        sArtistsProjectionMap.put(ArtistCache.Artists.COL_ARTISTID,
                ArtistCache.Artists.COL_ARTISTID);
        sArtistsProjectionMap.put(ArtistCache.Artists.COL_NAME, ArtistCache.Artists.COL_NAME);

        /*
         * Creates an initializes a projection map for handling Live Folders
         */

        // Creates a new projection map instance
        sLiveFolderProjectionMap = new HashMap<String, String>();

        // Maps "_ID" to "_ID AS _ID" for a live folder
        sLiveFolderProjectionMap.put(LiveFolders._ID, ArtistCache.Artists._ID + " AS "
                + LiveFolders._ID);

        // Maps "NAME" to "title AS NAME"
        sLiveFolderProjectionMap.put(LiveFolders.NAME, ArtistCache.Artists.COL_NAME + " AS " +
                LiveFolders.NAME);
    }

    class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME_SUFFIX = "-artist_cache.db";
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
            Log.v(TAG, "onCreate()");
            db.execSQL("CREATE TABLE " + ArtistCache.Artists.TABLE_NAME + " ("
                    + ArtistCache.Artists.COL_SERVERORDER + " INTEGER PRIMARY KEY,"
                    + ArtistCache.Artists.COL_ARTISTID + " TEXT,"
                    + ArtistCache.Artists.COL_NAME + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.v(TAG, "onUpgrade()");
            db.execSQL("DROP TABLE IF EXISTS " + ArtistCache.Artists.TABLE_NAME + ";");
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

            // Rebuild the cache if necessary
            maybeRebuildCache(db, false);

            return db;
        }

        /**
         * @param db
         * @param force Force the rebuild, irrespective of the server state
         */
        public synchronized void maybeRebuildCache(SQLiteDatabase db, boolean force) {
            final String uuid = mServerState.getUuid();

            if (uuid == null)
                throw new IllegalStateException(
                        "maybeRebuildCache() called when server has no uuid");

            // Check the cache is still valid, re-create if necessary
            final String lastScanKey = uuid + ":lastscan";
            final int oldLastScan = preferences.getInt(lastScanKey, -1);
            final int curLastScan = mServerState.getLastScan();

            /**
             * Recreate the database if:
             * <p>
             * force is true, or
             * <p>
             * we haven't got any record of the last scan time, or we do, and
             * they don't match.
             */
            if (force || oldLastScan == -1 || oldLastScan != curLastScan) {
                Log.v(TAG, "Rebuilding artist cache... " + oldLastScan + " : " + curLastScan);
                InsertHelper ih = new InsertHelper(db, ArtistCache.Artists.TABLE_NAME);
                final int serverOrderColumn = ih
                        .getColumnIndex(ArtistCache.Artists.COL_SERVERORDER);
                final int totalArtists = mServerState.getTotalArtists();

                onUpgrade(db, 0, 1);

                db.beginTransaction();
                try {
                    for (int i = 0; i < totalArtists; i++) {
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

                getContext().getContentResolver().notifyChange(ArtistCache.Artists.CONTENT_URI,
                        null);

                // Remove the cache directory
                if (mCacheDirectory.exists())
                    for (File f : mCacheDirectory.listFiles())
                        f.delete();

                mCacheDirectory.mkdirs();

                Editor editor = preferences.edit();
                editor.putInt(lastScanKey, curLastScan);
                editor.commit();

                getContext().getContentResolver().notifyChange(ArtistCache.Artists.CONTENT_URI,
                        null);

                Log.v(TAG, "... artist cache rebuilt");
            }
        }
    }

    /**
     * Every two seconds check to see if the content resolver should be notified
     * about updates, and if it should, send the notification.
     */
    public ArtistCacheProvider() {
        notifyUpdatesThreadPool.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (notifyUpdates.getAndSet(false) == true)
                    getContext().getContentResolver().notifyChange(ArtistCache.Artists.CONTENT_URI,
                            null);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "ArtistCacheProvider::onCreate running");
        Context context = getContext();

        Intent intent = new Intent(context, SqueezeService.class);
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public ArtistCacheCursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs,
            String sortOrder) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(ArtistCache.Artists.TABLE_NAME);

        /**
         * Choose the projection and adjust the "where" clause based on URI
         * pattern-matching.
         */
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI is for artists, chooses the Artists
            // projection
            case ARTISTS:
                qb.setProjectionMap(sArtistsProjectionMap);
                break;

            /*
             * If the incoming URI is for a single artist identified by its ID,
             * chooses the artist ID projection, and appends "_ID = <artistID>"
             * to the where clause, so that it selects that single artist.
             */
            case ARTIST_ID:
                qb.setProjectionMap(sArtistsProjectionMap);
                qb.appendWhere(
                        ArtistCache.Artists._ID + // the name of the ID column
                                "=" +
                                // the position of the artist ID itself in the
                                // incoming URI
                                uri.getPathSegments().get(
                                        ArtistCache.Artists.ARTIST_ID_PATH_POSITION));
                break;

            case LIVE_FOLDER_ARTISTS:
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
            orderBy = ArtistCache.Artists.DEFAULT_SORT_ORDER;
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

        return new ArtistCacheCursor(c, this);
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

            // If the pattern is for artists or live folders, returns the
            // general
            // content type.
            case ARTISTS:
            case LIVE_FOLDER_ARTISTS:
                return ArtistCache.Artists.CONTENT_TYPE;

                // If the pattern is for artist IDs, returns the artist ID
                // content
                // type.
            case ARTIST_ID:
                return ArtistCache.Artists.CONTENT_ITEM_TYPE;

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
        if (sUriMatcher.match(uri) != ARTISTS)
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
        long rowId = db.insert(ArtistCache.Artists.TABLE_NAME, null, values);

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID
            // appended to it.
            Uri artistUri = ContentUris.withAppendedId(ArtistCache.Artists.CONTENT_ID_URI_BASE,
                    rowId);

            // Notifies observers registered against this provider that the data
            // changed.
            getContext().getContentResolver().notifyChange(artistUri, null);
            return artistUri;
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
            // If the incoming URI matches the general artists pattern, does the
            // update based on the incoming data.
            case ARTISTS:

                // Does the update and returns the number of rows updated.
                count = db.update(
                        ArtistCache.Artists.TABLE_NAME, // The database table
                                                      // name.
                        values, // A map of column names and new values to use.
                        where, // The where clause column names.
                        whereArgs // The where clause column values to select
                                  // on.
                        );
                break;

            // If the incoming URI matches a single artists ID, does the update
            // based on the incoming data, but modifies the where clause to
            // restrict it to the particular artist ID.
            case ARTIST_ID:
                /*
                 * Starts creating the final WHERE clause by restricting it to
                 * the incoming note ID.
                 */
                finalWhere =
                        ArtistCache.Artists._ID + // The ID column name
                                " = " + // test for equality
                                uri.getPathSegments(). // the incoming note ID
                                        get(ArtistCache.Artists.ARTIST_ID_PATH_POSITION);

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (where != null)
                    finalWhere = finalWhere + " AND " + where;

                // Does the update and returns the number of rows updated.
                count = db.update(
                        ArtistCache.Artists.TABLE_NAME, // The database table
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

    private final IServiceArtistListCallback artistListCallback = new IServiceArtistListCallback.Stub() {
        /**
         * Update the affected rows.
         *
         * @param count Number of items as reported by squeezeserver.
         * @param start The start position of items in this update.
         * @param items New items to update in the cache
         */
        public void onArtistsReceived(int count, int start, List<SqueezerArtist> items)
                throws RemoteException {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            ContentValues cv = new ContentValues();
            int serverOrder = start;
            SqueezerArtist thisArtist;

            db.beginTransaction();
            try {
                Iterator<SqueezerArtist> it = items.iterator();
                while (it.hasNext()) {
                    thisArtist = it.next();
                    cv.put(ArtistCache.Artists.COL_NAME, thisArtist.getName());
                    cv.put(ArtistCache.Artists.COL_ARTISTID, thisArtist.getId());

                    db.update(ArtistCache.Artists.TABLE_NAME, cv,
                            ArtistCache.Artists.COL_SERVERORDER + "=?",
                            new String[] {
                                Integer.toString(serverOrder)
                            });

                    serverOrder++;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(ArtistCache.Artists.CONTENT_URI, null);
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
            Log.v(TAG, "onServerStateChanged");
            mServerState = newState;
            String uuid = mServerState.getUuid();

            Log.v(TAG, "ArtistCacheProvider: Server UUID is now " + uuid);

            if (uuid != null)
                mCacheDirectory = new File(Environment.getExternalStorageDirectory(),
                        "/Android/data/com.danga.squeezer/cache/artist/"
                                + mServerState.getUuid());
            else
                mCacheDirectory = null;

            mOpenHelper = new DatabaseHelper(getContext(), mServerState);
        }
    };

    /**
     * Possibly fetch the requested page of data from the service, if it's not
     * previously been requested.
     *
     * @param page The index of the page to fetch.
     */
    public void maybeRequestPage(int page) {
        if (mOrderedPages.contains(page))
            return;

        /*
         * Check to see if we already have an artistID for the first artist in
         * this page of data. If we do then there's no need to fetch.
         */
        Cursor c = query(ContentUris.withAppendedId(Artists.CONTENT_URI, page * mPageSize),
                new String[] {
                    ArtistCache.Artists.COL_ARTISTID
                }, null, null, "");

        // Might get 0 rows if the page is past the end of the data set.
        if (c.getCount() == 0)
            return;

        c.moveToFirst();
        final String artistId = c.getString(0);
        if (artistId != null) {
            mOrderedPages.add(page);
            return;
        }

        try {
            mOrderedPages.add(page);
            service.artists(page * mPageSize, null, null, null);
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
        mOrderedArtwork.clear();
    }
}
