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

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import org.jetbrains.annotations.Contract;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;

@AutoValue
public abstract class AlarmPlaylist extends Item {

    /** tag="category", category under which the playlist is grouped. */
    public abstract String category();

    /** tag="singleton", true if the item is the only one in its category, false otherwise. */
    public abstract boolean singleton();

    @NonNull
    @Contract(" -> !null")
    private static AlarmPlaylist.Builder builder() {
        return new AutoValue_AlarmPlaylist.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(final String id);
        public abstract Builder name(final String name);
        public abstract Builder category(final String category);
        public abstract Builder singleton(final boolean singleton);
        public abstract AlarmPlaylist build();
    }

    @NonNull
    public static AlarmPlaylist fromMap(@NonNull Map<String, String> record) {
        return AlarmPlaylist.builder()
                .id(record.get("url"))
                .name(record.get("title"))
                .category(record.get("category"))
                .singleton(Util.parseDecimalIntOrZero(record.get("singleton")) == 1)
                .build();
    }
}
