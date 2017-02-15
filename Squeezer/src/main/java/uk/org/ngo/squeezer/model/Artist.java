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

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;


public class Artist extends PlaylistItem {

    @NonNull
    @Override
    public String getPlaylistTag() {
        return "artist_id";
    }

    @NonNull
    @Override
    public String getFilterTag() {
        return "artist_id";
    }

    private String mName;

    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    public Artist setName(String name) {
        mName = name;
        return this;
    }

    public Artist(String artistId, String artist) {
        setId(artistId);
        setName(artist);
    }

    public Artist(@NonNull Map<String, String> record) {
        setId(record.containsKey("contributor_id") ? record.get("contributor_id")
                : record.get("id"));
        mName = record.containsKey("contributor") ? record.get("contributor") : record.get("artist");
    }

    public static final Creator<Artist> CREATOR = new Creator<Artist>() {
        @NonNull
        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }

        @NonNull
        @Override
        public Artist createFromParcel(@NonNull Parcel source) {
            return new Artist(source);
        }
    };

    private Artist(@NonNull Parcel source) {
        setId(source.readString());
        mName = source.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mName);
    }

}
