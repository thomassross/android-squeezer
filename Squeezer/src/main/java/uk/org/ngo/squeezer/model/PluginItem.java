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
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import org.jetbrains.annotations.Contract;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;

/**
 * Represents a single item in a plugin.
 */
@AutoValue
public abstract class PluginItem extends Item {
    /** tag="hasitems", true if this item has sub-items. */
    public abstract boolean hasitems();

    /** tag="isaudio", true if the item is audio. */
    public abstract boolean isAudio();

    /**
     * tag="type", stream content type, "link" means subitems must be fetched.
     * Expected values include "link", "text", "audio", "playlist".
     */
    @NonNull
    public abstract String type();

    /** tag="description", more information about the plugin. */
    @NonNull
    public abstract String description();

    /** tag="image", Relative URL to the icon to use for this item. */
    @Nullable
    public abstract String image();

    @NonNull
    @Contract(" -> !null")
    private static Builder builder() {
        return new AutoValue_PluginItem.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract Builder id(final String id);
        abstract Builder name(final String name);
        abstract Builder description(final String description);
        abstract Builder image(final String image);
        abstract Builder hasitems(final boolean hasitems);
        abstract Builder type(final String type);
        abstract Builder isAudio(final boolean isAudio);
        abstract PluginItem build();
    }

    @NonNull
    public static PluginItem fromMap(@NonNull Map<String, String> record) {
        return PluginItem.builder()
                .id(record.get("id"))
                .name(record.containsKey("name") ? record.get("name") : record.get("title"))
                .description(Strings.nullToEmpty(record.get("description")))
                .image(record.get("image"))
                .hasitems(Util.parseDecimalIntOrZero(record.get("hasitems")) != 0)
                .type(Strings.nullToEmpty(record.get("type")))
                .isAudio(Util.parseDecimalIntOrZero(record.get("isaudio")) != 0)
                .build();
    }
}

