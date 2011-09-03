
package com.danga.squeezer.service;

import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;

import com.danga.squeezer.R;
import com.danga.squeezer.Squeezer;

public class GenreCacheCursor extends CursorWrapper {
    @SuppressWarnings("unused")
    private static final String TAG = GenreCacheCursor.class.getName();

    public static final int TYPE_LIVEUPDATE = 0;
    public static final int TYPE_REQUEST_PAGE = 1;

    private final Cursor mCursor;
    private final GenreCacheProvider mProvider;

    /** Whether queries should fetch from the server for missing data. */
    private Boolean mLiveUpdate = true;

    private final int mPageSize = Squeezer.getContext().getResources()
            .getInteger(R.integer.PageSize);

    /** Map column names to default values for those columns. */
    private static HashMap<String, String> defaults = new HashMap<String, String>();
    {
        // TODO: Localise
        defaults.put(GenreCache.Genres.COL_NAME, "Loading...");
    }

    public GenreCacheCursor(Cursor cursor, GenreCacheProvider provider) {
        super(cursor);

        mCursor = cursor;
        mProvider = provider;
    }

    @Override
    public String getString(int columnIndex) {
        String theString = mCursor.getString(columnIndex);

        if (theString == null) {
            String thisColumnName = getColumnName(columnIndex);

            // Name? If so, kick off a fetch.
            if (thisColumnName.equals(GenreCache.Genres.COL_NAME)) {
                if (mLiveUpdate)
                    requestPagesFromPositions(getPosition(), getPosition());

                return defaults.get(GenreCache.Genres.COL_NAME);
            }

            // Got a default value for it? If so, return it.
            if (defaults.containsKey(thisColumnName))
                return defaults.get(thisColumnName);

            // Everything else? Return the null.
            return null;
        }

        return theString;
    }


    /**
     * @param firstPosition
     * @param lastPosition
     */
    private void requestPagesFromPositions(int firstPosition, int lastPosition) {
        int firstPage = firstPosition / mPageSize;
        int lastPage = lastPosition / mPageSize;

        mProvider.maybeRequestPage(firstPage);

        if (lastPage != firstPage)
            mProvider.maybeRequestPage(lastPage);

        // If we're more than halfway through the current page then request the
        // next one as well
        if (lastPosition % mPageSize > mPageSize / 2)
            mProvider.maybeRequestPage(lastPage + 1);
    }

    @Override
    public Bundle respond(Bundle extras) {
        int type = extras.getInt("TYPE");
        switch (type) {
            case TYPE_LIVEUPDATE:
                mLiveUpdate = extras.getBoolean("LiveUpdate");
                break;

            case TYPE_REQUEST_PAGE:
                int firstPosition = extras.getInt("firstPosition");
                int lastPosition = extras.getInt("lastPosition");
                requestPagesFromPositions(firstPosition, lastPosition);
                break;

            default:
                throw new IllegalArgumentException("Unknown value in TYPE field of Bundle: " + type);
        }

        return Bundle.EMPTY;
    }
}
