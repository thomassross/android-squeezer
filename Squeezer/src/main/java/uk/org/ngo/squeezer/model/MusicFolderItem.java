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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import org.jetbrains.annotations.Contract;

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;

/**
 * Encapsulate a music folder item on the Squeezeserver.
 * <p>
 * An item has a name and a type. The name is free text, the type may be one of "track", "folder",
 * "playlist", or "unknown".
 */
@AutoValue
public abstract class MusicFolderItem extends PlaylistItem implements Parcelable {

    @NonNull
    @Override
    public String playlistTag() {
        if ("track".equals(type())) {
            return "track_id";
        }

        if ("playlist".equals(type())) {
            return "playlist_id";
        }

        if ("folder".equals(type())) {
            return "folder_id";
        }

        return "Unknown_type_in_playlistTag()";
    }

    @NonNull
    @Override
    public String filterTag() {
        return "folder_id";
    }

    /** tag="type", the folder item's type, "track", "folder", "playlist", "unknown". */
    public abstract String type();

    @NonNull
    public abstract Uri url();

    /** tag="download_url", the URL to use to download the song. */
    @NonNull
    public abstract Uri downloadUrl();

    @Contract("_ -> !null")
    public static MusicFolderItem fromMap(@NonNull Map<String, String> record) {
        return new AutoValue_MusicFolderItem(
                record.get("id"),
                record.get("filename"),
                record.get("type"),
                Uri.parse(Strings.nullToEmpty(record.get("url"))),
                Uri.parse(Strings.nullToEmpty(record.get("download_url"))));
    }

    @Override
    public String intentExtraKey() {
        return MusicFolderItem.class.getName();
    }
}
