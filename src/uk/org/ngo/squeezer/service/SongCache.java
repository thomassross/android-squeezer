
package uk.org.ngo.squeezer.service;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Represent the song cache database.
 */
public final class SongCache {
    public static final String AUTHORITY = "uk.org.ngo.squeezer.service.SongCache";

    private SongCache() {
    }

    public static final class Songs implements BaseColumns {
        private Songs() {
        }

        public static final String TABLE_NAME = "song";

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the Songs URI
         */
        private static final String PATH = "/songs";

        /**
         * Path part for the Song ID URI
         */
        private static final String PATH_ID = "/songs/";

        /**
         * 0-relative position of an song ID segment in the path part of a song
         * ID URI
         */
        public static final int ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH);

        /**
         * The content URI base for a single song. Callers must append a numeric
         * song id to this Uri to retrieve an song.
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_ID);

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.danga.squeezer.song";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String ITEM_CONTENT_TYPE = "vnd.android.cursor.item/vnd.danga.squeezer.song";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_SONGID = "songid";
        public static final String COL_NAME = "name";
        public static final String COL_ARTIST = "artist";
        public static final String COL_YEAR = "year";
        public static final String COL_ARTWORK_TRACK_ID = "artwork_id";
        public static final String COL_ARTWORK_PATH = "artwork_path";
    }
}
