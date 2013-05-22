package uk.org.ngo.squeezer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents query parameters for a server query.
 * 
 */
public class QueryParameters {
    public String searchString;
    public SqueezerAlbum album;
    public SqueezerArtist artist;
    public SqueezerYear year;
    public SqueezerGenre genre;

    public String toString() {
        List<String> parameters = new ArrayList<String>();

        if (searchString != null && searchString.length() > 0)
            parameters.add("search:" + searchString);
        if (album != null)
            parameters.add("album_id:" + album.getId());
        if (artist != null)
            parameters.add("artist_id:" + artist.getId());
        if (year != null)
            parameters.add("year:" + year.getId());
        if (genre != null)
            parameters.add("genre_id:" + genre.getId());

        return parameters.toString();
    }
}
