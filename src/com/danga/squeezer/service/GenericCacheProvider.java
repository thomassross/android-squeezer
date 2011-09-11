
package com.danga.squeezer.service;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
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
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.danga.squeezer.Preferences;
import com.danga.squeezer.R;
import com.danga.squeezer.Squeezer;

public abstract class GenericCacheProvider extends ContentProvider {
    private static final String TAG = GenericCacheProvider.class.getName();

    SqueezerServerState mServerState;

    protected ISqueezeService service;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
            GenericCacheProvider.this.onServiceConnected(name, binder);
        }

        public void onServiceDisconnected(ComponentName name) {
            service = null;
        };
    };

    /**
     * Connected to the service. Subclasses should register any callbacks.
     *
     * @param name
     * @param binder
     */
    abstract void onServiceConnected(ComponentName name, IBinder binder);

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

    /** The number of items per page. */
    protected final int mPageSize = Squeezer.getContext().getResources()
            .getInteger(R.integer.PageSize);

    /**
     * @return The squeezeservice, or null if not bound
     */
    public ISqueezeService getService() {
        return service;
    }

    /** A projection map used to select columns from the database. */
    protected HashMap<String, String> sProjectionMap;

    /** A projection map used to select columns from the database. */
    protected HashMap<String, String> sLiveFolderProjectionMap;

    /*
     * Constants used by the Uri matcher to choose an action based on the
     * pattern of the incoming URI
     */
    protected static final int ALL_ENTRIES = 1;
    protected static final int SINGLE_ENTRY = 2;
    protected static final int LIVE_FOLDER = 3;

    /** A UriMatcher instance. */
    protected UriMatcher sUriMatcher;

    protected DatabaseHelper mOpenHelper;

    /**
     * A representation of the directory that the provider uses as the root of
     * the cache.
     */
    protected File mCacheDirectory;

    protected SqueezerTable mTableDefinition;

    class DatabaseHelper extends SQLiteOpenHelper {
        private final SharedPreferences preferences;

        DatabaseHelper(Context context, String name, int version) {
            super(context, name, null, version);

            preferences = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.v(TAG, "onCreate()");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.v(TAG, "onUpgrade()");
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
                Log.v(TAG, "Rebuilding cache... " + oldLastScan + " : " + curLastScan);
                InsertHelper ih = new InsertHelper(db, mTableDefinition.TABLE_NAME);
                final int serverOrderColumn = ih
                        .getColumnIndex(mTableDefinition.COL_SERVERORDER);
                final int totalRows = getTotalRows();

                onUpgrade(db, 0, 1);

                db.beginTransaction();
                try {
                    for (int i = 0; i < totalRows; i++) {
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

                getContext().getContentResolver().notifyChange(mTableDefinition.CONTENT_URI, null);

                // Remove the cache directory
                if (mCacheDirectory.exists())
                    for (File f : mCacheDirectory.listFiles())
                        f.delete();

                mCacheDirectory.mkdirs();

                Editor editor = preferences.edit();
                editor.putInt(lastScanKey, curLastScan);
                editor.commit();

                getContext().getContentResolver().notifyChange(mTableDefinition.CONTENT_URI, null);

                Log.v(TAG, "... cache rebuilt");
            }
        }
    }

    /**
     * Returns the total number of rows that should be pre-allocated in the
     * database.
     * <p>
     * Subclasses should override this and return the result of calling the
     * correct getTotal*() method on the service.
     *
     * @return
     */
    abstract int getTotalRows();

    @Override
    public boolean onCreate() {
        Log.v(TAG, "onCreate running");
        Context context = getContext();

        /**
         * Every two seconds check to see if the content resolver should be
         * notified about updates, and if it should, send the notification.
         */
        notifyUpdatesThreadPool.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (notifyUpdates.getAndSet(false) == true)
                    getContext().getContentResolver().notifyChange(mTableDefinition.CONTENT_URI,
                            null);
            }
        }, 2, 2, TimeUnit.SECONDS);

        Intent intent = new Intent(context, SqueezeService.class);
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs,
            String sortOrder) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(mTableDefinition.TABLE_NAME);

        /**
         * Choose the projection and adjust the "where" clause based on URI
         * pattern-matching.
         */
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI is for all items, use the normal projection
            // map.
            case ALL_ENTRIES:
                qb.setProjectionMap(sProjectionMap);
                break;

            /*
             * If the incoming URI is for a single item identified by its ID,
             * choose normal projection, and append "_ID = <itemID>" to the
             * where clause, so that it selects that single item.
             */
            case SINGLE_ENTRY:
                qb.setProjectionMap(sProjectionMap);
                qb.appendWhere(
                        mTableDefinition.COL_SERVERORDER + "="
                                + uri.getPathSegments().get(mTableDefinition.ID_PATH_POSITION));
                break;

            case LIVE_FOLDER:
                // If the incoming URI is from a live folder, chooses the live
                // folder projection.
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;

            default:
                // If the URI doesn't match any of the known patterns, throw an
                // exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified, uses the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder))
            orderBy = mTableDefinition.DEFAULT_SORT_ORDER;
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

        return WrapCursor(c);
    }

    /**
     * Wrap the supplied Cursor with a cursor of a different type.
     * <p>
     * Subclasses should override this and return a cursor of the correct type
     * for the concrete cache.
     *
     * @param c The cursor to wrap.
     * @return The wrapped cursor.
     */
    abstract Cursor WrapCursor(Cursor c);

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        throw new UnsupportedOperationException();
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

            case ALL_ENTRIES:
            case LIVE_FOLDER:
                // If the pattern is for all items or live folders, returns the
                // general content type.
                return mTableDefinition.CONTENT_TYPE;

            case SINGLE_ENTRY:
                // If the pattern is for individual IDs, returns the item
                // content type.
                return mTableDefinition.ITEM_CONTENT_TYPE;

            default:
                // If the URI pattern doesn't match any permitted patterns,
                // throw an exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validates the incoming URI. Only the full provider URI is allowed for
        // inserts.
        if (sUriMatcher.match(uri) != ALL_ENTRIES)
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
        long rowId = db.insert(mTableDefinition.TABLE_NAME, null, values);

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the item ID pattern and the new row ID
            // appended to it.
            Uri u = ContentUris.withAppendedId(mTableDefinition.CONTENT_ID_URI_BASE,
                    rowId);

            // Notifies observers registered against this provider that the data
            // changed.
            getContext().getContentResolver().notifyChange(u, null);
            return u;
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
            // If the incoming URI matches the general items pattern, does the
            // update based on the incoming data.
            case ALL_ENTRIES:

                // Does the update and returns the number of rows updated.
                count = db.update(mTableDefinition.TABLE_NAME, values,
                        where, whereArgs);
                break;

            // If the incoming URI matches a single items ID, does the update
            // based on the incoming data, but modifies the where clause to
            // restrict it to the particular item ID.
            case SINGLE_ENTRY:
                /*
                 * Starts creating the final WHERE clause by restricting it to
                 * the incoming ID.
                 */
                finalWhere =
                        mTableDefinition.COL_SERVERORDER + " = "
                                + uri.getPathSegments().get(mTableDefinition.ID_PATH_POSITION);

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (where != null)
                    finalWhere = finalWhere + " AND " + where;

                // Does the update and returns the number of rows updated.
                count = db.update(mTableDefinition.TABLE_NAME, values,
                        finalWhere, whereArgs);
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
         * Check to see if the mandatory column contains data. If it does then
         * there's no need to fetch.
         */
        Cursor c = query(
                ContentUris.withAppendedId(mTableDefinition.CONTENT_URI, page * mPageSize),
                new String[] {
                    mTableDefinition.MANDATORY_COLUMN
                }, null, null, "");

        // Might get 0 rows if the page is past the end of the data set.
        if (c.getCount() == 0)
            return;

        c.moveToFirst();
        final String text = c.getString(0);
        if (text != null) {
            mOrderedPages.add(page);
            return;
        }

        try {
            mOrderedPages.add(page);
            requestPage(page * mPageSize);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            mOrderedPages.remove(page);
            e.printStackTrace();
        }
    }

    /**
     * Request a page of data from the service.
     * <p>
     * Subclasses should call the appropriate method on the service.
     *
     * @param page
     * @throws RemoteException
     */
    abstract void requestPage(int page) throws RemoteException;

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
