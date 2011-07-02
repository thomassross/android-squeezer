
package com.danga.squeezer.service;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Represent the album cache database.
 */
public final class AlbumCache {
    public static final String AUTHORITY = "com.danga.squeezer.service.AlbumCache";

    private AlbumCache() {
    }

    public static final class Albums implements BaseColumns {
        private Albums() {
        }

        public static final String TABLE_NAME = "album";

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the Albums URI
         */
        private static final String PATH_ALBUMS = "/albums";

        /**
         * Path part for the Album ID URI
         */
        private static final String PATH_ALBUM_ID = "/albums/";

        /**
         * 0-relative position of an album ID segment in the path part of a
         * album ID URI
         */
        public static final int ALBUM_ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_ALBUMS);

        /**
         * The content URI base for a single album. Callers must append a
         * numeric album id to this Uri to retrieve an album.
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_ALBUM_ID);

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.danga.squeezer.album";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.danga.squeezer.album";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_ALBUMID = "albumid";
        public static final String COL_NAME = "name";
        public static final String COL_ARTIST = "artist";
        public static final String COL_YEAR = "year";
        public static final String COL_ARTWORK_TRACK_ID = "artwork_id";
        public static final String COL_ARTWORK_PATH = "artwork_path";
    }
}
