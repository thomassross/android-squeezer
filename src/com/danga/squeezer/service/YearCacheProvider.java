
package com.danga.squeezer.service;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.LiveFolders;
import android.util.Log;

import com.danga.squeezer.itemlists.IServiceYearListCallback;
import com.danga.squeezer.model.SqueezerYear;

public class YearCacheProvider extends GenericCacheProvider {
    private static final String TAG = YearCacheProvider.class.getName();

    /**
     * Register the artistListCallback.
     *
     * @param name
     * @param binder
     */
    @Override
    void onServiceConnected(ComponentName name, IBinder binder) {
        try {
            service.registerYearListCallback(yearListCallback);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static final class Years implements BaseColumns {
        private Years() {
        }

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_YEARID = "yearid";
        public static final String COL_NAME = "name";
    }

    class DatabaseHelper extends GenericCacheProvider.DatabaseHelper {
        private static final String DATABASE_NAME_SUFFIX = "-year_cache.db";
        private static final int DATABASE_VERSION = 1;

        DatabaseHelper(Context context, SqueezerServerState serverState) {
            super(context, serverState.getUuid() + DATABASE_NAME_SUFFIX, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            super.onCreate(db);
            db.execSQL("CREATE TABLE " + ProviderUri.YEAR.getTableName() + " ("
                    + Years.COL_SERVERORDER + " INTEGER PRIMARY KEY,"
                    + Years.COL_YEARID + " TEXT,"
                    + Years.COL_NAME + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            super.onUpgrade(db, oldVersion, newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + ProviderUri.YEAR.getTableName() + ";");
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        mTableDefinition = ProviderUri.YEAR;

        sUriMatcher = ProviderUri.YEAR.getUriMatcher();

        sProjectionMap = new HashMap<String, String>();
        sProjectionMap.put(Years.COL_SERVERORDER, Years.COL_SERVERORDER);
        sProjectionMap.put(Years.COL_YEARID, Years.COL_YEARID);
        sProjectionMap.put(Years.COL_NAME, Years.COL_NAME);

        sLiveFolderProjectionMap = new HashMap<String, String>();
        sLiveFolderProjectionMap.put(LiveFolders._ID, Years._ID + " AS "
                + LiveFolders._ID);
        sLiveFolderProjectionMap.put(LiveFolders.NAME, Years.COL_NAME + " AS " +
                LiveFolders.NAME);

        return super.onCreate();

    }

    @Override
    Cursor WrapCursor(Cursor c) {
        return new YearCacheCursor(c, this);
    }

    @Override
    int getTotalRows() {
        return mServerState.getTotalYears();
    }

    private final IServiceYearListCallback yearListCallback = new IServiceYearListCallback.Stub() {
        /**
         * Update the affected rows.
         *
         * @param count Number of items as reported by squeezeserver.
         * @param start The start position of items in this update.
         * @param items New items to update in the cache
         */
        public void onYearsReceived(int count, int start, List<SqueezerYear> items)
                throws RemoteException {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            ContentValues cv = new ContentValues();
            int serverOrder = start;
            SqueezerYear thisYear;

            db.beginTransaction();
            try {
                Iterator<SqueezerYear> it = items.iterator();
                while (it.hasNext()) {
                    thisYear = it.next();
                    cv.put(Years.COL_NAME, thisYear.getName());
                    cv.put(Years.COL_YEARID, thisYear.getId());

                    db.update(ProviderUri.YEAR.getTableName(), cv,
                            Years.COL_SERVERORDER + "=?",
                            new String[] {
                                Integer.toString(serverOrder)
                            });

                    serverOrder++;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(ProviderUri.YEAR.getContentUri(), null);
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
            Log.v(TAG, "onServerStateChanged");
            mServerState = newState;
            String uuid = mServerState.getUuid();

            Log.v(TAG, "YearCacheProvider: Server UUID is now " + uuid);

            if (uuid != null)
                mCacheDirectory = new File(Environment.getExternalStorageDirectory(),
                        "/Android/data/com.danga.squeezer/cache/year/"
                                + mServerState.getUuid());
            else
                mCacheDirectory = null;

            mOpenHelper = new DatabaseHelper(getContext(), mServerState);
        }
    };

    @Override
    void requestPage(int page) throws RemoteException {
        service.years(page);
    }
}
