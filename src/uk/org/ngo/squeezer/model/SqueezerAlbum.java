/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.model;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.SqueezerArtworkItem;
import uk.org.ngo.squeezer.service.AlbumCache.Albums;
import android.database.Cursor;
import android.os.Parcel;


public class SqueezerAlbum extends SqueezerArtworkItem {

    @Override
    public String getPlaylistTag() {
        return "album_id";
    }

	private String name;
	@Override public String getName() { return name; }
	public SqueezerAlbum setName(String name) { this.name = name; return this; }

    private String artist;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String model) {
        this.artist = model;
    }

    private int year;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    private String mArtworkPath;

    public String getArtworkPath() {
        return mArtworkPath;
    }

    public void setArtworkPath(String artworkPath) {
        this.mArtworkPath = artworkPath;
    }

	public SqueezerAlbum(String albumId, String album) {
		setId(albumId);
		setName(album);
	}

	public SqueezerAlbum(Map<String, String> record) {
		setId(record.containsKey("album_id") ? record.get("album_id") : record.get("id"));
		setName(record.get("album"));
		setArtist(record.get("artist"));
		setYear(Util.parseDecimalIntOrZero(record.get("year")));
		setArtwork_track_id(record.get("artwork_track_id"));
	}

    /**
     * Construct from a cursor.
     * <p>
     * Assumes the cursor is pointing at the correct row, and contains (at
     * least) COL_ALBUMID and COL_NAME columns. Other columns will be included
     * as necessary.
     *
     * @param cursor
     */
    public SqueezerAlbum(Cursor cursor) {
        setId(cursor.getString(cursor.getColumnIndex(Albums.COL_ALBUMID)));
        setName(cursor.getString(cursor.getColumnIndex(Albums.COL_NAME)));

        int i;
        i = cursor.getColumnIndex(Albums.COL_ARTIST);
        if (i != -1)
            setArtist(cursor.getString(i));

        i = cursor.getColumnIndex(Albums.COL_YEAR);
        if (i != -1)
            setYear(Util.parseDecimalIntOrZero(cursor.getString(i)));

        i = cursor.getColumnIndex(Albums.COL_ARTWORK_PATH);
        if (i != -1)
            setArtworkPath(cursor.getString(i));
    }

    public static final Creator<SqueezerAlbum> CREATOR = new Creator<SqueezerAlbum>() {
        public SqueezerAlbum[] newArray(int size) {
            return new SqueezerAlbum[size];
        }

		public SqueezerAlbum createFromParcel(Parcel source) {
			return new SqueezerAlbum(source);
		}
	};

	private SqueezerAlbum(Parcel source) {
		setId(source.readString());
		name = source.readString();
		artist = source.readString();
		year = source.readInt();
		setArtwork_track_id(source.readString());
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(artist);
		dest.writeInt(year);
		dest.writeString(getArtwork_track_id());
	}


	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
	}

}
