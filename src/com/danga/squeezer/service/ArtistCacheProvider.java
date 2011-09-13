
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

import com.danga.squeezer.itemlists.IServiceArtistListCallback;
import com.danga.squeezer.model.SqueezerArtist;

public class ArtistCacheProvider extends GenericCacheProvider {
    private static final String TAG = ArtistCacheProvider.class.getName();

    /**
     * Register the artistListCallback.
     *
     * @param name
     * @param binder
     */
    @Override
    void onServiceConnected(ComponentName name, IBinder binder) {
        try {
            service.registerArtistListCallback(artistListCallback);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static final class Artists implements BaseColumns {
        private Artists() {
        }

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_ARTISTID = "artistid";
        public static final String COL_NAME = "name";
    }

    class DatabaseHelper extends GenericCacheProvider.DatabaseHelper {
        private static final String DATABASE_NAME_SUFFIX = "-artist_cache.db";
        private static final int DATABASE_VERSION = 1;

        DatabaseHelper(Context context, SqueezerServerState serverState) {
            super(context, serverState.getUuid() + DATABASE_NAME_SUFFIX, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            super.onCreate(db);
            db.execSQL("CREATE TABLE " + ProviderUri.ARTIST.getTableName() + " ("
                    + Artists.COL_SERVERORDER + " INTEGER PRIMARY KEY,"
                    + Artists.COL_ARTISTID + " TEXT,"
                    + Artists.COL_NAME + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            super.onUpgrade(db, oldVersion, newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + ProviderUri.ARTIST.getTableName() + ";");
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        mTableDefinition = ProviderUri.ARTIST;

        sUriMatcher = ProviderUri.ARTIST.getUriMatcher();

        sProjectionMap = new HashMap<String, String>();
        sProjectionMap.put(Artists.COL_SERVERORDER, Artists.COL_SERVERORDER);
        sProjectionMap.put(Artists.COL_ARTISTID, Artists.COL_ARTISTID);
        sProjectionMap.put(Artists.COL_NAME, Artists.COL_NAME);

        sLiveFolderProjectionMap = new HashMap<String, String>();
        sLiveFolderProjectionMap.put(LiveFolders._ID, Artists._ID + " AS "
                + LiveFolders._ID);
        sLiveFolderProjectionMap.put(LiveFolders.NAME, Artists.COL_NAME + " AS " +
                LiveFolders.NAME);

        return super.onCreate();
    }

    @Override
    Cursor WrapCursor(Cursor c) {
        return new ArtistCacheCursor(c, this);
    }

    @Override
    int getTotalRows() {
        return mServerState.getTotalArtists();
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
                    cv.put(Artists.COL_NAME, thisArtist.getName());
                    cv.put(Artists.COL_ARTISTID, thisArtist.getId());

                    db.update(ProviderUri.ARTIST.getTableName(), cv,
                            Artists.COL_SERVERORDER + "=?",
                            new String[] {
                                Integer.toString(serverOrder)
                            });

                    serverOrder++;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(ProviderUri.ARTIST.getContentUri(),
                    null);
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
            Log.v(TAG, "onServerStateChanged");
            mServerState = newState;
            String uuid = mServerState.getUuid();

            Log.v(TAG, "Server UUID is now " + uuid);

            if (uuid != null)
                mCacheDirectory = new File(Environment.getExternalStorageDirectory(),
                        "/Android/data/com.danga.squeezer/cache/artist/"
                                + mServerState.getUuid());
            else
                mCacheDirectory = null;

            mOpenHelper = new DatabaseHelper(getContext(), mServerState);
        }
    };

    @Override
    void requestPage(int page) throws RemoteException {
        service.artists(page, null, null, null);
    }
}
