
package com.danga.squeezer.service;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Represent the album cache database.
 */
public final class ArtistCache {
    public static final String AUTHORITY = "com.danga.squeezer.service.ArtistCache";

    private ArtistCache() {
    }

    public static final class Artists implements BaseColumns {
        private Artists() {
        }

        public static final String TABLE_NAME = "artist";

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path part for the Artists URI
         */
        private static final String PATH_ARTISTS = "/artists";

        /**
         * Path part for the Artist ID URI
         */
        private static final String PATH_ARTIST_ID = "/artists/";

        /**
         * 0-relative position of an artist ID segment in the path part of a
         * artist ID URI
         */
        public static final int ARTIST_ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_ARTISTS);

        /**
         * The content URI base for a single artist. Callers must append a
         * numeric artist id to this Uri to retrieve an artist.
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri
                .parse(SCHEME + AUTHORITY + PATH_ARTIST_ID);

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.danga.squeezer.artist";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.danga.squeezer.artist";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String COL_SERVERORDER = android.provider.BaseColumns._ID;
        public static final String COL_ARTISTID = "artistid";
        public static final String COL_NAME = "name";
    }
}
