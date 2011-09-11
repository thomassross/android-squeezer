
package com.danga.squeezer.service;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Represent the album cache database.
 */
public final class GenreCache {
    public static final String AUTHORITY = "com.danga.squeezer.service.GenreCache";

    private GenreCache() {
    }

    public static final class Genres implements BaseColumns {
        private Genres() {
        }

        public static final String TABLE_NAME = "genre";

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the Genres URI
         */
        private static final String PATH = "/genres";

        /**
         * Path part for the Genre ID URI
         */
        private static final String PATH_ID = "/genres/";

        /**
         * 0-relative position of an genre ID segment in the path part of a
         * genre ID URI
         */
        public static final int ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH);

        /**
         * The content URI base for a single genre. Callers must append a
         * numeric genre id to this Uri to retrieve an genre.
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri
                .parse(SCHEME + AUTHORITY + PATH_ID);

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.danga.squeezer.genre";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String ITEM_CONTENT_TYPE = "vnd.android.cursor.item/vnd.danga.squeezer.genre";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_GENREID = "genreid";
        public static final String COL_NAME = "name";
    }
}
