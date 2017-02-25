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
import com.google.common.base.Strings;

import org.jetbrains.annotations.Contract;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.ArtworkItem;

@AutoValue
public abstract class Album extends ArtworkItem implements Parcelable {
    @NonNull
    @Override
    public String playlistTag() {
        return "album_id";
    }

    @NonNull
    @Override
    public String filterTag() {
        return "album_id";
    }

    /** tag="artist", album artist, or "Various" if compilation tag is set. */
    @Nullable
    public abstract String artist();

    /** tag="year", album year */
    public abstract int year();

    @NonNull
    public abstract Uri artworkUrl();

    @NonNull
    @Contract(" -> !null")
    private static Builder builder() {
        return new AutoValue_Album.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(final String id);
        public abstract Builder name(final String name);
        public abstract Builder artist(final String artist);
        public abstract Builder year(final int year);
        public abstract Builder artworkUrl(final Uri artworkUrl);
        public abstract Builder artworkTrackId(final String artworkTrackId);
        public abstract Album build();
    }

    @NonNull
    public static Album fromMap(@NonNull Map<String, String> record) {
        return Album.builder()
                .id(record.containsKey("album_id") ? record.get("album_id") : record.get("id"))
                .name(record.get("album"))
                .artist(Util.parseDecimalIntOrZero(record.get("compilation")) == 1?
                        "Various" : record.get("artist"))
                .year(Util.parseDecimalIntOrZero(record.get("year")))
                .artworkUrl(Uri.parse(Strings.nullToEmpty(record.get("artwork_url"))))
                .artworkTrackId(Strings.nullToEmpty(record.get("artwork_track_id")))
                .build();
    }

    @Override
    public String intentExtraKey() {
        return Album.class.getName();
    }
}
