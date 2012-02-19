
package uk.org.ngo.squeezer.service;

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

import uk.org.ngo.squeezer.itemlists.IServiceGenreListCallback;
import uk.org.ngo.squeezer.model.SqueezerGenre;

public class GenreCacheProvider extends GenericCacheProvider {
    private static final String TAG = GenreCacheProvider.class.getName();;

    public static final class Genres implements BaseColumns {
        private Genres() {
        }

        public static final String TABLE_NAME = "genre";

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_GENREID = "genreid";
        public static final String COL_NAME = "name";
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        try {
            service.registerGenreListCallback(genreListCallback);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    class DatabaseHelper extends GenericCacheProvider.DatabaseHelper {
        private static final String DATABASE_NAME_SUFFIX = "-genre_cache.db";
        private static final int DATABASE_VERSION = 1;

        DatabaseHelper(Context context, SqueezerServerState serverState) {
            super(context, serverState.getUuid() + DATABASE_NAME_SUFFIX, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            super.onCreate(db);
            db.execSQL("CREATE TABLE " + ProviderUri.GENRE.getTableName() + " ("
                    + Genres.COL_SERVERORDER + " INTEGER PRIMARY KEY,"
                    + Genres.COL_GENREID + " TEXT,"
                    + Genres.COL_NAME + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            super.onUpgrade(db, oldVersion, newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + ProviderUri.GENRE.getTableName() + ";");
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        mTableDefinition = ProviderUri.GENRE;

        sUriMatcher = ProviderUri.GENRE.getUriMatcher();

        sProjectionMap = new HashMap<String, String>();
        sProjectionMap.put(Genres.COL_SERVERORDER, Genres.COL_SERVERORDER);
        sProjectionMap.put(Genres.COL_GENREID, Genres.COL_GENREID);
        sProjectionMap.put(Genres.COL_NAME, Genres.COL_NAME);

        sLiveFolderProjectionMap = new HashMap<String, String>();
        sLiveFolderProjectionMap.put(LiveFolders._ID, Genres._ID + " AS "
                + LiveFolders._ID);
        sLiveFolderProjectionMap.put(LiveFolders.NAME, Genres.COL_NAME + " AS " +
                LiveFolders.NAME);

        return super.onCreate();
    }

    @Override
    Cursor WrapCursor(Cursor c) {
        return new GenreCacheCursor(c, this);
    }

    @Override
    int getTotalRows() {
        return mServerState.getTotalGenres();
    }

    private final IServiceGenreListCallback genreListCallback = new IServiceGenreListCallback.Stub() {
        /**
         * Update the affected rows.
         *
         * @param count Number of items as reported by squeezeserver.
         * @param start The start position of items in this update.
         * @param items New items to update in the cache
         */
        public void onGenresReceived(int count, int start, List<SqueezerGenre> items)
                throws RemoteException {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            ContentValues cv = new ContentValues();
            int serverOrder = start;
            SqueezerGenre thisGenre;

            db.beginTransaction();
            try {
                Iterator<SqueezerGenre> it = items.iterator();
                while (it.hasNext()) {
                    thisGenre = it.next();
                    cv.put(Genres.COL_NAME, thisGenre.getName());
                    cv.put(Genres.COL_GENREID, thisGenre.getId());

                    db.update(ProviderUri.GENRE.getTableName(), cv,
                            Genres.COL_SERVERORDER + "=?",
                            new String[] {
                                Integer.toString(serverOrder)
                            });

                    serverOrder++;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            getContext().getContentResolver().notifyChange(ProviderUri.GENRE.getContentUri(),
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
                        "/Android/data/uk.org.ngo.squeezer/cache/genre/"
                                + mServerState.getUuid());
            else
                mCacheDirectory = null;

            mOpenHelper = new DatabaseHelper(getContext(), mServerState);
        }
    };

    @Override
    void requestPage(int page) throws RemoteException {
        service.genres(page);
    }
}
