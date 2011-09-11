package com.danga.squeezer.service;

import android.net.Uri;
import android.provider.BaseColumns;

public class SqueezerTable implements BaseColumns {
    public static String TABLE_NAME;

    /**
     * 0-relative position of an artist ID segment in the path part of a artist
     * ID URI
     */
    public int ID_PATH_POSITION = 1;

    /**
     * The content:// style URL for this table
     */
    public static Uri CONTENT_URI;

    /**
     * The content URI base for a single item. Callers must append a numeric
     * item id to this Uri to retrieve an item.
     */
    public static Uri CONTENT_ID_URI_BASE;

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
     */
    public String CONTENT_TYPE;

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
     */
    public String ITEM_CONTENT_TYPE;

    /**
     * The default sort order for this table
     */
    public String DEFAULT_SORT_ORDER = "_id ASC";

    public String COL_SERVERORDER = android.provider.BaseColumns._ID;

    /**
     * The name of a column that must be non-null before we can safely assume
     * the rest of the row's content is sensible.
     */
    public String MANDATORY_COLUMN;
}
