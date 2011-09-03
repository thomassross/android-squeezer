
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
import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.service.SongCache.Songs;
import com.google.android.panoramio.BitmapUtils;

public class SongCacheProvider extends ContentProvider {
    private static final String TAG = "SongCacheProvider";

    private SqueezerServerState mServerState;

    private ISqueezeService service;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
            try {
                service.registerSongListCallback(songListCallback);
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
    private static HashMap<String, String> sSongsProjectionMap;

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sLiveFolderProjectionMap;

    /*
     * Constants used by the Uri matcher to choose an action based on the
     * pattern of the incoming URI
     */
    // The incoming URI matches the Songs URI pattern
    private static final int SONGS = 1;

    // The incoming URI matches the Song ID URI pattern
    private static final int SONG_ID = 2;

    // The incoming URI matches the Live Folder URI pattern
    private static final int LIVE_FOLDER_SONGS = 3;

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

        // Add a pattern that routes URIs terminated with "songs" to an SONGS
        // operation
        sUriMatcher.addURI(SongCache.AUTHORITY, "songs", SONGS);

        // Add a pattern that routes URIs terminated with "songs" plus an
        // integer
        // to an song ID operation
        sUriMatcher.addURI(SongCache.AUTHORITY, "songs/#", SONG_ID);

        // Add a pattern that routes URIs terminated with live_folders/songs to
        // a
        // live folder operation
        sUriMatcher.addURI(SongCache.AUTHORITY, "live_folders/songs", LIVE_FOLDER_SONGS);

        /*
         * Creates and initializes a projection map that returns all columns
         */

        // Creates a new projection map instance. The map returns a column name
        // given a string. The two are usually equal.
        sSongsProjectionMap = new HashMap<String, String>();

        sSongsProjectionMap.put(SongCache.Songs.COL_SERVERORDER,
                SongCache.Songs.COL_SERVERORDER);
        sSongsProjectionMap.put(SongCache.Songs.COL_SONGID, SongCache.Songs.COL_SONGID);
        sSongsProjectionMap.put(SongCache.Songs.COL_NAME, SongCache.Songs.COL_NAME);
        sSongsProjectionMap.put(SongCache.Songs.COL_ARTIST, SongCache.Songs.COL_ARTIST);
        sSongsProjectionMap.put(SongCache.Songs.COL_YEAR, SongCache.Songs.COL_YEAR);
        sSongsProjectionMap
                .put(SongCache.Songs.COL_ARTWORK_TRACK_ID, SongCache.Songs.COL_ARTWORK_TRACK_ID);
        sSongsProjectionMap.put(SongCache.Songs.COL_ARTWORK_PATH,
                SongCache.Songs.COL_ARTWORK_PATH);
        sSongsProjectionMap.put("_data", "_data");

        /*
         * Creates an initializes a projection map for handling Live Folders
         */

        // Creates a new projection map instance
        sLiveFolderProjectionMap = new HashMap<String, String>();

        // Maps "_ID" to "_ID AS _ID" for a live folder
        sLiveFolderProjectionMap.put(LiveFolders._ID, SongCache.Songs._ID + " AS "
                + LiveFolders._ID);

        // Maps "NAME" to "title AS NAME"
        sLiveFolderProjectionMap.put(LiveFolders.NAME, SongCache.Songs.COL_NAME + " AS " +
                LiveFolders.NAME);
    }

    class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME_SUFFIX = "-song_cache.db";
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
            db.execSQL("CREATE TABLE " + SongCache.Songs.TABLE_NAME + " ("
                    + SongCache.Songs.COL_SERVERORDER + " INTEGER PRIMARY KEY,"
                    + SongCache.Songs.COL_SONGID + " TEXT,"
                    + SongCache.Songs.COL_NAME + " TEXT,"
                    + SongCache.Songs.COL_ARTIST + " TEXT,"
                    + SongCache.Songs.COL_YEAR + " TEXT,"
                    + SongCache.Songs.COL_ARTWORK_TRACK_ID + " TEXT,"
                    + SongCache.Songs.COL_ARTWORK_PATH + " TEXT,"
                    + "_data TEXT" // Song artwork location metadata
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.v(TAG, "onUpgrade()");
            db.execSQL("DROP TABLE IF EXISTS " + SongCache.Songs.TABLE_NAME + ";");
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
                Log.v(TAG, "Rebuilding song cache... " + oldLastScan + " : " + curLastScan);
                InsertHelper ih = new InsertHelper(db, SongCache.Songs.TABLE_NAME);
                final int serverOrderColumn = ih.getColumnIndex(SongCache.Songs.COL_SERVERORDER);
                final int totalSongs = mServerState.getTotalSongs();

                onUpgrade(db, 0, 1);

                db.beginTransaction();
                try {
                    for (int i = 0; i < totalSongs; i++) {
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

                getContext().getContentResolver().notifyChange(SongCache.Songs.CONTENT_URI, null);

                // Remove the cache directory
                if (mCacheDirectory.exists())
                    for (File f : mCacheDirectory.listFiles())
                        f.delete();

                mCacheDirectory.mkdirs();

                Editor editor = preferences.edit();
                editor.putInt(lastScanKey, curLastScan);
                editor.commit();

                getContext().getContentResolver().notifyChange(SongCache.Songs.CONTENT_URI, null);

                Log.v(TAG, "... song cache rebuilt");
            }
        }
    }

    /**
     * Every two seconds check to see if the content resolver should be notified
     * about updates, and if it should, send the notification.
     */
    public SongCacheProvider() {
        notifyUpdatesThreadPool.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (notifyUpdates.getAndSet(false) == true)
                    getContext().getContentResolver().notifyChange(SongCache.Songs.CONTENT_URI,
                            null);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "SongCacheProvider::onCreate running");
        Context context = getContext();

        Intent intent = new Intent(context, SqueezeService.class);
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public SongCacheCursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs,
            String sortOrder) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SongCache.Songs.TABLE_NAME);

        /**
         * Choose the projection and adjust the "where" clause based on URI
         * pattern-matching.
         */
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI is for songs, chooses the Songs projection
            case SONGS:
                qb.setProjectionMap(sSongsProjectionMap);
                break;

            /*
             * If the incoming URI is for a single song identified by its ID,
             * chooses the song ID projection, and appends "_ID = <songID>" to
             * the where clause, so that it selects that single song.
             */
            case SONG_ID:
                qb.setProjectionMap(sSongsProjectionMap);
                qb.appendWhere(
                        SongCache.Songs._ID + // the name of the ID column
                                "=" +
                                // the position of the song ID itself in the
                                // incoming URI
                                uri.getPathSegments().get(SongCache.Songs.SONG_ID_PATH_POSITION));
                break;

            case LIVE_FOLDER_SONGS:
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
            orderBy = SongCache.Songs.DEFAULT_SORT_ORDER;
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

        return new SongCacheCursor(c, this);
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

            // If the pattern is for songs or live folders, returns the general
            // content type.
            case SONGS:
            case LIVE_FOLDER_SONGS:
                return SongCache.Songs.CONTENT_TYPE;

                // If the pattern is for song IDs, returns the song ID content
                // type.
            case SONG_ID:
                return SongCache.Songs.CONTENT_ITEM_TYPE;

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
        if (sUriMatcher.match(uri) != SONGS)
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
        long rowId = db.insert(SongCache.Songs.TABLE_NAME, null, values);

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID
            // appended to it.
            Uri songUri = ContentUris.withAppendedId(SongCache.Songs.CONTENT_ID_URI_BASE, rowId);

            // Notifies observers registered against this provider that the data
            // changed.
            getContext().getContentResolver().notifyChange(songUri, null);
            return songUri;
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
            // If the incoming URI matches the general songs pattern, does the
            // update based on the incoming data.
            case SONGS:

                // Does the update and returns the number of rows updated.
                count = db.update(
                        SongCache.Songs.TABLE_NAME, // The database table
                                                      // name.
                        values, // A map of column names and new values to use.
                        where, // The where clause column names.
                        whereArgs // The where clause column values to select
                                  // on.
                        );
                break;

            // If the incoming URI matches a single songs ID, does the update
            // based on the incoming data, but modifies the where clause to
            // restrict it to the particular song ID.
            case SONG_ID:
                /*
                 * Starts creating the final WHERE clause by restricting it to
                 * the incoming note ID.
                 */
                finalWhere =
                        SongCache.Songs._ID + // The ID column name
                                " = " + // test for equality
                                uri.getPathSegments(). // the incoming note ID
                                        get(SongCache.Songs.SONG_ID_PATH_POSITION);

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (where != null)
                    finalWhere = finalWhere + " AND " + where;

                // Does the update and returns the number of rows updated.
                count = db.update(
                        SongCache.Songs.TABLE_NAME, // The database table
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

    private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
        /**
         * Update the affected rows.
         *
         * @param count Number of items as reported by squeezeserver.
         * @param start The start position of items in this update.
         * @param items New items to update in the cache
         */
        public void onSongsReceived(int count, int start, List<SqueezerSong> items)
                throws RemoteException {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            ContentValues cv = new ContentValues();
            int serverOrder = start;
            SqueezerSong thisSong;

            db.beginTransaction();
            try {
                Iterator<SqueezerSong> it = items.iterator();
                while (it.hasNext()) {
                    thisSong = it.next();
                    cv.put(SongCache.Songs.COL_NAME, thisSong.getName());
                    cv.put(SongCache.Songs.COL_SONGID, thisSong.getId());
                    cv.put(SongCache.Songs.COL_ARTIST, thisSong.getArtist());
                    cv.put(SongCache.Songs.COL_YEAR, thisSong.getYear());
                    cv.put(SongCache.Songs.COL_ARTWORK_TRACK_ID, thisSong.getArtwork_track_id());

                    db.update(SongCache.Songs.TABLE_NAME, cv,
                            SongCache.Songs.COL_SERVERORDER + "=?",
                            new String[] {
                                Integer.toString(serverOrder)
                            });

                    serverOrder++;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(SongCache.Songs.CONTENT_URI, null);
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
            Log.v(TAG, "onServerStateChanged");
            mServerState = newState;
            String uuid = mServerState.getUuid();

            Log.v(TAG, "SongCacheProvider: Server UUID is now " + uuid);

            if (uuid != null)
                mCacheDirectory = new File(Environment.getExternalStorageDirectory(),
                        "/Android/data/com.danga.squeezer/cache/song/"
                                + mServerState.getUuid());
            else
                mCacheDirectory = null;

            mOpenHelper = new DatabaseHelper(getContext(), mServerState);
        }

        public void onItemsFinished() throws RemoteException {
            // TODO Auto-generated method stub

        }
    };


    private String getSongArtUrl(String artwork_track_id) {
        if (artwork_track_id == null)
            return null;

        if (service == null)
            return null;

        try {
            return service.getAlbumArtUrl(artwork_track_id);
        } catch (RemoteException e) {
            Log.e(TAG, "Error requesting song art url: " + e);
            return null;
        }
    }

    /**
	 *
	 */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        if (sUriMatcher.match(uri) != SONG_ID)
            throw new IllegalArgumentException("openFile not supported for multiple songs");

        return openFileHelper(uri, mode);
    }

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
         * Check to see if we already have an songID for the first song in this
         * page of data. If we do then there's no need to fetch.
         */
        Cursor c = query(ContentUris.withAppendedId(Songs.CONTENT_URI, page * mPageSize),
                new String[] {
                    SongCache.Songs.COL_SONGID
                }, null, null, "");

        // Might get 0 rows if the page is past the end of the data set.
        if (c.getCount() == 0)
            return;

        c.moveToFirst();
        final String songId = c.getString(0);
        if (songId != null) {
            mOrderedPages.add(page);
            return;
        }

        try {
            mOrderedPages.add(page);
            service.songs(page * mPageSize, "tracknum", null, null, null, null, null);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            mOrderedPages.remove(page);
            e.printStackTrace();
        }
    }

    /**
     * Request the artwork for a given song.
     *
     * @param position The song position (note: not page) in the list.
     */
    public void requestArtwork(final int position) {
        artworkThreadPool.execute(new Runnable() {
            public void run() {
                // Do nothing if we're already ordering it.
                if (mOrderedArtwork.contains(position)) {
                    return;
                }

                // Determine the URL for the song artwork. Check the song ID
                // and the track ID -- no song ID means we know nothing about
                // this song yet.
                //
                // This means the song hasn't been fetched. Rely on the fact
                // that something else should already be in the process of
                // fetching it.
                Cursor c = query(ContentUris.withAppendedId(Songs.CONTENT_URI, position),
                        new String[] {
                                SongCache.Songs.COL_SONGID,
                                SongCache.Songs.COL_ARTWORK_TRACK_ID
                    }, null, null, "");

                c.moveToFirst();
                final String songId = c.getString(0);

                // No songId means no song.
                if (songId == null || songId.length() == 0) {
                    return;
                }

                // Figure out the URL for the artwork.
                final String artworkTrackId = c.getString(1);
                final String songArtUrl = getSongArtUrl(artworkTrackId);
                c.close();

                // If we know the song, and there's no artwork URL then no
                // artwork exists. Update the database to use the default song
                // artwork, and return
                if (songArtUrl == null || songArtUrl.length() == 0) {
                    mOrderedArtwork.add(position);

                    ContentValues cv = new ContentValues();
                    cv.put(SongCache.Songs.COL_ARTWORK_PATH,
                            Integer.toString(R.drawable.icon_album_noart_143));

                    update(ContentUris.withAppendedId(Songs.CONTENT_URI, position),
                            cv, null, null);

                    // Database content changed, a notification should
                    // be sent 'soon'
                    notifyUpdates.set(true);

                    return;
                }

                try {
                    mOrderedArtwork.add(position);

                    if (!mCacheDirectory.exists())
                        if (!mCacheDirectory.mkdirs())
                            return;

                    File artwork = new File(mCacheDirectory, artworkTrackId + ".jpg");
                    File artwork64 = new File(mCacheDirectory, artworkTrackId + "-64px.png");

                    // File artwork = new File(root, trackId + "/original.png");
                    // File artwork64 = new File(root, trackId +
                    // "/scaled-64px.png");

                    if (artwork.createNewFile()) {
                        InputStream in = null;
                        BufferedOutputStream out = null;
                        BufferedOutputStream out64 = null;

                        // Read the remote image, copy to disk
                        in = new BufferedInputStream(new URL(songArtUrl).openStream(),
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
                        cv.put(SongCache.Songs.COL_ARTWORK_PATH,
                                SongCache.Songs.CONTENT_ID_URI_BASE
                                        + Integer.toString(position));
                        cv.put("_data", artwork64.getAbsolutePath());

                        update(ContentUris.withAppendedId(Songs.CONTENT_URI, position),
                                cv, null, null);

                        // Database content changed, a notification should
                        // be sent 'soon'
                        notifyUpdates.set(true);
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    mOrderedArtwork.remove(position);
                    e.printStackTrace();
                } catch (IOException e) {
                    mOrderedArtwork.remove(position);
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
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
