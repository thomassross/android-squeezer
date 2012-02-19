package uk.org.ngo.squeezer.service;

import uk.org.ngo.squeezer.service.YearCacheProvider.Years;

import android.content.UriMatcher;
import android.net.Uri;

public enum ProviderUri {
    ARTIST(
            "artist",
            "content://", "uk.org.ngo.squeezer.service.ArtistCache", "artists",
            1,
            "vnd.android.cursor.dir/vnd.danga.squeezer.artist",
            "vnd.android.cursor.item/vnd.danga.squeezer.artist",
            "_id ASC", ArtistCacheProvider.Artists.COL_ARTISTID
    ),
    GENRE(
            "genre",
            "content://", "uk.org.ngo.squeezer.service.GenreCache", "genres",
            1,
            "vnd.android.cursor.dir/vnd.danga.squeezer.genre",
            "vnd.android.cursor.item/vnd.danga.squeezer.genre",
            "_id ASC", GenreCacheProvider.Genres.COL_GENREID
    ),
    YEAR(
            "year",
            "content://", "uk.org.ngo.squeezer.service.YearCache", "years",
            1,
            "vnd.android.cursor.dir/vnd.danga.squeezer.year",
            "vnd.android.cursor.item/vnd.danga.squeezer.year",
            "_id ASC", YearCacheProvider.Years.COL_YEARID
    );

    private final String authority;

    /** The database table name. */
    private final String tableName;

    /** The scheme part for this provider's URI. */
    private final String scheme;

    /** Path part for the provider's URI. */
    private final String path;

    /** Path part for the item ID URI. */
    private final String pathId;

    /**
     * 0-relative position of an item ID segment in the path part of an item ID
     * URI
     */
    private final int idPathPosition;

    /** The content:// style URL for this table */
    private final Uri contentUri;

    /**
     * The content URI base for a single item. Callers must append a numeric
     * item id to this URI to retrieve an item.
     */
    private final Uri contentIdUriBase;

    /** The MIME type of {@link #CONTENT_URI} providing a directory of notes. */
    private final String contentType;

    /** The MIME type of a {@link #CONTENT_URI} sub-directory of a single note. */
    private final String itemContentType;

    /** The default sort order for this table */
    private final String defaultSortOrder;

    private final String colServerOrder = android.provider.BaseColumns._ID;

    private final String mandatoryColumn;

    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    /*
     * Constants used by the Uri matcher to choose an action based on the
     * pattern of the incoming URI
     */
    public static final int ALL_ENTRIES = 1;
    public static final int SINGLE_ENTRY = 2;
    public static final int LIVE_FOLDER = 3;

    ProviderUri(String tableName, String scheme, String authority, String path,
            int idPathPosition, String contentType,
            String itemContentType, String defaultSortOrder,
            String mandatoryColumn) {
        this.authority = authority;
        this.tableName = tableName;

        this.scheme = scheme;
        this.path = "/" + path;
        this.pathId = this.path + "/";

        this.idPathPosition = idPathPosition;

        this.contentUri = Uri.parse(scheme + authority + this.path);
        this.contentIdUriBase = Uri.parse(scheme + authority + this.pathId);

        // Route URIs appropriately.  Use path, not this.path, as it shouldn't
        // have the leading '/'.
        this.uriMatcher.addURI(authority, path, ALL_ENTRIES);
        this.uriMatcher.addURI(authority, path + "/#", SINGLE_ENTRY);
        this.uriMatcher.addURI(authority, "live_folders/" + path, LIVE_FOLDER);

        this.contentType = contentType;
        this.itemContentType = itemContentType;

        this.defaultSortOrder = defaultSortOrder;
        this.mandatoryColumn = mandatoryColumn;
    }

    /**
     * @return the authority
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return the scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the pathId
     */
    public String getPathId() {
        return pathId;
    }

    /**
     * @return the idPathPosition
     */
    public int getIdPathPosition() {
        return idPathPosition;
    }

    /**
     * @return the contentUri
     */
    public Uri getContentUri() {
        return contentUri;
    }

    /**
     * @return the contentIdUriBase
     */
    public Uri getContentIdUriBase() {
        return contentIdUriBase;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the itemContentType
     */
    public String getItemContentType() {
        return itemContentType;
    }

    /**
     * @return the defaultSortOrder
     */
    public String getDefaultSortOrder() {
        return defaultSortOrder;
    }

    /**
     * @return the colServerOrder
     */
    public String getColServerOrder() {
        return colServerOrder;
    }

    /**
     * @return the mandatoryColumn
     */
    public String getMandatoryColumn() {
        return mandatoryColumn;
    }

    /**
     * @return the uriMatcher
     */
    public UriMatcher getUriMatcher() {
        return uriMatcher;
    }
}
