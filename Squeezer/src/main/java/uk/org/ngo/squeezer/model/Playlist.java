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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import org.jetbrains.annotations.Contract;

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;

@AutoValue
public abstract class Playlist extends PlaylistItem {
    @NonNull
    @Override
    public String playlistTag() {
        return "playlist_id";
    }

    @NonNull
    @Override
    public String filterTag() {
        return "playlist_id";
    }

    @NonNull
    @Contract(" -> !null")
    private static Builder builder() {
        return new AutoValue_Playlist.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(final String id);
        public abstract Builder name(final String name);
        public abstract Playlist build();
    }

    @NonNull
    public static Playlist fromMap(@NonNull Map<String, String> record) {
        return Playlist.builder()
                .id(record.containsKey("playlist_id") ? record.get("playlist_id") : record.get("id"))
                .name(record.get("playlist"))
                .build();
    }

    @CheckResult
    public abstract Playlist withName(String name);
}
