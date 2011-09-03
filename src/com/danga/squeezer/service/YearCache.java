
package com.danga.squeezer.service;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Represent the album cache database.
 */
public final class YearCache {
    public static final String AUTHORITY = "com.danga.squeezer.service.YearCache";

    private YearCache() {
    }

    public static final class Years implements BaseColumns {
        private Years() {
        }

        public static final String TABLE_NAME = "year";

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the Years URI
         */
        private static final String PATH_YEARS = "/years";

        /**
         * Path part for the Year ID URI
         */
        private static final String PATH_YEAR_ID = "/years/";

        /**
         * 0-relative position of an year ID segment in the path part of a year
         * ID URI
         */
        public static final int YEAR_ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_YEARS);

        /**
         * The content URI base for a single year. Callers must append a numeric
         * year id to this Uri to retrieve an year.
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri
                .parse(SCHEME + AUTHORITY + PATH_YEAR_ID);

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.danga.squeezer.year";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.danga.squeezer.year";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_YEARID = "yearid";
        public static final String COL_NAME = "name";
    }
}
