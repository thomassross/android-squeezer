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


public class Playlist extends PlaylistItem {

    @NonNull
    @Override
    public String getPlaylistTag() {
        return "playlist_id";
    }

    @NonNull
    @Override
    public String getFilterTag() {
        return "playlist_id";
    }

    private String mName;

    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    public Playlist setName(String name) {
        mName = name;
        return this;
    }

    public Playlist(@NonNull Map<String, String> record) {
        setId(record.containsKey("playlist_id") ? record.get("playlist_id") : record.get("id"));
        mName = record.get("playlist");
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @NonNull
        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }

        @NonNull
        @Override
        public Playlist createFromParcel(@NonNull Parcel source) {
            return new Playlist(source);
        }
    };

    private Playlist(@NonNull Parcel source) {
        setId(source.readString());
        mName = source.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mName);
    }

}
