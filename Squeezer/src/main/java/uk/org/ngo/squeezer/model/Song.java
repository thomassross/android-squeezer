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

import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.download.DownloadFilenameStructure;
import uk.org.ngo.squeezer.download.DownloadPathStructure;
import uk.org.ngo.squeezer.framework.ArtworkItem;

/**
 * Used to handle songs returned by the "songs", "playlists tracks",
 * "search", and "status" queries (see calls to
 * {@link uk.org.ngo.squeezer.service.CliClient.SongListHandler})
 * </p>
 * The list of fields returned for a song are controlled by the value of
 * {@link uk.org.ngo.squeezer.service.SqueezeService#SONGTAGS}.
 */
@AutoValue
public abstract class Song extends ArtworkItem implements Parcelable {
    @NonNull
    @Override
    public String playlistTag() {
        return "track_id";
    }

    @NonNull
    @Override
    public String filterTag() {
        return "track_id";
    }

    /** tag="artist", artist name. */
    @Nullable
    public abstract String artist();

    /** tag="compilation", true if the song is part of a compilation. */
    public abstract boolean compilation();

    /** tag="duration", duration of the song, in seconds. */
    public abstract int duration();

    /** tag="album_id", ID of the album. May be null if song is not associated with an album. */
    @Nullable
    public abstract String albumId();

    /** tag="coverart", true if coverart is available for the song. */
    public abstract boolean coverart();

    /**
     * tag="artwork_url", full URL to remote artwork, for remote songs. Created by
     * CliClient.addArtworkUrlTag
     */
    public abstract Uri artworkUrl();

    /** tag="album", name of the album the song is part of. May be null. */
    public abstract String albumName();

    /** tag="artist_id", ID of the artist. */
    @Nullable
    public abstract String artistId();

    /** tag="tracknum", track number of the song on the album. */
    public abstract int trackNum();

    /** tag="remote", true if this is a remote song (i.e., being streamed). */
    public abstract boolean remote();

    /** tag="url", URL of the track on the server. This is the file:/// URL,
     * not the URL to download it.
     */
    @NonNull public abstract Uri url();

    /** tag="year", year of the song (if known). */
    public abstract int year();

    /** tag="download_url", The URL to use to download the song. */
    @Nullable public abstract Uri downloadUrl();

    /** tag="buttons", for remote songs indicates that track control buttons are available. */
    @NonNull public abstract String buttons();

    /** Album object for the song. */
    @NonNull public abstract Album album();

    /**
     * @return Whether the song has artwork associated with it.
     */
    public boolean hasArtwork() {
        return ! (artworkUrl().equals(Uri.EMPTY));
    }

    @NonNull
    @Contract(" -> !null")
    private static Builder builder() {
        return new AutoValue_Song.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        public abstract Builder id(final String id);
        public abstract Builder name(final String name);
        public abstract Builder artist(final String artist);
        public abstract Builder compilation(final boolean compilation);
        public abstract Builder duration(final int duration);
        public abstract Builder albumId(final String albumId);
        public abstract Builder coverart(final boolean coverart);
        public abstract Builder artworkTrackId(final String artworkTrackId);
        public abstract Builder artworkUrl(final Uri artworkUrl);
        public abstract Builder albumName(final String albumName);
        public abstract Builder artistId(final String artistId);
        public abstract Builder trackNum(final int trackNum);
        public abstract Builder remote(final boolean remote);
        public abstract Builder url(final Uri url);
        public abstract Builder year(final int year);
        public abstract Builder downloadUrl(final Uri downloadUrl);
        public abstract Builder buttons(final String buttons);
        public abstract Builder album(final Album album);
        public abstract Song build();
    }

    @NonNull
    public static Song fromMap(@NonNull Map<String, String> record) {
        return Song.builder()
                // TODO: Explain why this might be "id" or "track_id".
                .id(record.containsKey("track_id") ? record.get("track_id") : record.get("id"))
                // TODO: Explain why this might be "track" or "title".
                .name(record.containsKey("track") ? Strings.nullToEmpty(record.get("track"))
                        : Strings.nullToEmpty(record.get("title")))
                .artist(record.get("artist"))
                .artistId(Strings.nullToEmpty(record.get("artist_id")))
                .albumName(Strings.nullToEmpty(record.get("album")))
                .albumId(Strings.nullToEmpty(record.get("album_id")))
                .compilation(Util.parseDecimalIntOrZero(record.get("compilation")) == 1)
                .coverart(Util.parseDecimalIntOrZero(record.get("coverart")) == 1)
                .duration(Util.parseDecimalIntOrZero(record.get("duration")))
                .year(Util.parseDecimalIntOrZero(record.get("year")))
                .remote(Util.parseDecimalIntOrZero(record.get("remote")) != 0)
                .trackNum(Util.parseDecimalInt(record.get("tracknum"), 1))
                .url(Uri.parse(Strings.nullToEmpty(record.get("url"))))
                .artworkUrl(Uri.parse(Strings.nullToEmpty(record.get("artwork_url"))))
                .downloadUrl(Uri.parse(Strings.nullToEmpty(record.get("download_url"))))
                .buttons(Strings.nullToEmpty(record.get("buttons")))
                .album(Album.fromMap(record))
                .build();
    }

    public String getLocalPath(DownloadPathStructure downloadPathStructure, DownloadFilenameStructure downloadFilenameStructure) {
        return new File(downloadPathStructure.get(this), downloadFilenameStructure.get(this)).getPath();
    }

    /*
     * Extend the equality test by looking at additional track information.
     * <p>
     * This is to deal with songs from remote streams where the stream might provide a single
     * song ID for multiple consecutive songs in the stream.
     *
     * @param o The object to test.
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        // super.equals() has already checked that o is not null and is of the same class.
        Song s = (Song) o;

        if (!s.name().equals(name())) {
            return false;
        }

        if (!s.albumName().equals(albumName())) {
            return false;
        }

        if (!s.artist().equals(artist())) {
            return false;
        }

        if (!s.artworkUrl().equals(artworkUrl())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id(), name(), albumName(), artist(), artworkUrl());
    }

    @Override
    public String intentExtraKey() {
        return Song.class.getName();
    }
}
