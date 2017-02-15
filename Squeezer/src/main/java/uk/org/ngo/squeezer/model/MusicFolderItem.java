/*
 * Copyright (c) 2012 Google Inc.
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
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Strings;

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;

/**
 * Encapsulate a music folder item on the Squeezeserver.
 * <p>
 * An item has a mName and a mType. The mName is free text, the mType may be one of "track", "folder",
 * "playlist", or "unknown".
 *
 * @author nik
 */
public class MusicFolderItem extends PlaylistItem {

    @NonNull
    @Override
    public String getPlaylistTag() {
        if ("track".equals(mType)) {
            return "track_id";
        }

        if ("playlist".equals(mType)) {
            return "playlist_id";
        }

        if ("folder".equals(mType)) {
            return "folder_id";
        }

        return "Unknown_type_in_getTag()";
    }

    @NonNull
    @Override
    public String getFilterTag() {
        return "folder_id";
    }

    private String mName;

    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    public MusicFolderItem setName(String name) {
        mName = name;
        return this;
    }

    /**
     * The folder item's mType, "track", "folder", "playlist", "unknown".
     */
    // XXX: Should be an enum.
    private String mType;

    public String getType() {
        return mType;
    }

    @NonNull
    public MusicFolderItem setType(String type) {
        mType = type;
        return this;
    }

    @NonNull
    private Uri mUrl = Uri.EMPTY;

    @NonNull
    public Uri getUrl() {
        return mUrl;
    }

    public void setUrl(@NonNull Uri url) {
        mUrl = url;
    }

    /** The URL to use to download the song. */
    @NonNull
    private Uri mDownloadUrl = Uri.EMPTY;

    @NonNull
    public Uri getDownloadUrl() {
        return mDownloadUrl;
    }

    public MusicFolderItem(@NonNull Map<String, String> record) {
        setId(record.get("id"));
        mName = record.get("filename");
        mType = record.get("mType");
        mUrl = Uri.parse(Strings.nullToEmpty(record.get("url")));
        mDownloadUrl = Uri.parse(Strings.nullToEmpty(record.get("download_url")));
    }

    public static final Creator<MusicFolderItem> CREATOR = new Creator<MusicFolderItem>() {
        @NonNull
        @Override
        public MusicFolderItem[] newArray(int size) {
            return new MusicFolderItem[size];
        }

        @NonNull
        @Override
        public MusicFolderItem createFromParcel(@NonNull Parcel source) {
            return new MusicFolderItem(source);
        }
    };

    private MusicFolderItem(@NonNull Parcel source) {
        setId(source.readString());
        mName = source.readString();
        mType = source.readString();
        mUrl = Uri.parse(source.readString());
        mDownloadUrl = Uri.parse(source.readString());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mName);
        dest.writeString(mType);
        dest.writeString(mUrl.toString());
        dest.writeString(mDownloadUrl.toString());
    }

    @NonNull
    @Override
    public String toStringOpen() {
        return super.toStringOpen() + ", mType: " + mType;
    }
}
