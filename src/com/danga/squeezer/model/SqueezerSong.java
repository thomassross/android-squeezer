package com.danga.squeezer.model;

import java.util.Map;

import android.database.Cursor;
import android.os.Parcel;

import com.danga.squeezer.Util;
import com.danga.squeezer.framework.SqueezerArtworkItem;
import com.danga.squeezer.service.SongCache.Songs;

public class SqueezerSong extends SqueezerArtworkItem {

	private String name;
    private String mArtworkPath;

	@Override public String getName() { return name; }
	public SqueezerSong setName(String name) { this.name = name; return this; }

	private String artist;
	public String getArtist() { return artist; }
	public void setArtist(String artist) { this.artist = artist; }

	private String album;
	public String getAlbum() { return album; }
	public void setAlbum(String album) { this.album = album; }

	private int year;
	public int getYear() { return year; }
	public void setYear(int year) { this.year = year; }

	private String artist_id;
	public String getArtist_id() { return artist_id; }
	public void setArtist_id(String artist_id) { this.artist_id = artist_id; }

	private String album_id;

	public String getAlbum_id() { return album_id; }
	public void setAlbum_id(String album_id) { this.album_id = album_id; }

    public String getArtworkPath() {
        return mArtworkPath;
    }

    public void setArtworkPath(String artworkPath) {
        this.mArtworkPath = artworkPath;
    }

	public SqueezerSong(Map<String, String> record) {
		if (getId() == null) setId(record.get("track_id"));
		if (getId() == null) setId(record.get("id"));
		setName(record.containsKey("track") ? record.get("track") : record.get("title"));
		setArtist(record.get("artist"));
		setAlbum(record.get("album"));
		setYear(Util.parseDecimalIntOrZero(record.get("year")));
		setArtist_id(record.get("artist_id"));
		setAlbum_id(record.get("album_id"));
		setArtwork_track_id(record.get("artwork_track_id"));
	}

    /**
     * Construct from a cursor.
     * <p>
     * Assumes the cursor is pointing at the correct row, and contains (at
     * least) COL_SONGID and COL_NAME columns. Other columns will be included as
     * necessary.
     *
     * @param cursor
     */
    public SqueezerSong(Cursor cursor) {
        setId(cursor.getString(cursor.getColumnIndex(Songs.COL_SONGID)));
        setName(cursor.getString(cursor.getColumnIndex(Songs.COL_NAME)));

        int i;
        i = cursor.getColumnIndex(Songs.COL_ARTIST);
        if (i != -1)
            setArtist(cursor.getString(i));

        i = cursor.getColumnIndex(Songs.COL_YEAR);
        if (i != -1)
            setYear(Util.parseDecimalIntOrZero(cursor.getString(i)));

        i = cursor.getColumnIndex(Songs.COL_ARTWORK_PATH);
        if (i != -1)
            setArtworkPath(cursor.getString(i));
    }

	public static final Creator<SqueezerSong> CREATOR = new Creator<SqueezerSong>() {
		public SqueezerSong[] newArray(int size) {
			return new SqueezerSong[size];
		}

		public SqueezerSong createFromParcel(Parcel source) {
			return new SqueezerSong(source);
		}
	};
	private SqueezerSong(Parcel source) {
		setId(source.readString());
		name = source.readString();
		artist = source.readString();
		album = source.readString();
		year = source.readInt();
		artist_id = source.readString();
		album_id = source.readString();
		setArtwork_track_id(source.readString());
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
		dest.writeString(name);
		dest.writeString(artist);
		dest.writeString(album);
		dest.writeInt(year);
		dest.writeString(artist_id);
		dest.writeString(album_id);
		dest.writeString(getArtwork_track_id());
	}


	@Override
	public String toString() {
		return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
	}

}
