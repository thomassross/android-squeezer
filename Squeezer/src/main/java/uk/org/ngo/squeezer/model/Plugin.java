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
import android.support.annotation.VisibleForTesting;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import org.jetbrains.annotations.Contract;

import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


@AutoValue
public abstract class Plugin extends Item {

    public static final Plugin FAVORITE = Plugin.create("favorites", R.drawable.ic_favorites);
    public static final Plugin MY_APPS = Plugin.create("myapps", R.drawable.ic_my_apps);

    /** Icon resource for this plugin if it is embedded in the Squeezer app, or 0. */
    public abstract int iconResource();

    /**
     * tag="icon", relative URL path to an icon for this radio or music service, for example
     * "plugins/Picks/html/images/icon.png"
     */
    @Nullable
    public abstract String icon();

    /** tag="weight", numeric weight to use when sorting results, sort lowest to highest. */
    public abstract int weight();

    /** tag="type", plugin type. Expected values include "xmlbrowser" and "xmlbrowser_search". */
    @NonNull
    public abstract String type();

    public boolean isSearchable() {
        return "xmlbrowser_search".equals(type());
    }

    @NonNull
    @Contract(" -> !null")
    private static Builder builder() {
        return new AutoValue_Plugin.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(final String id);
        abstract Builder name(final String name);
        abstract Builder iconResource(final int iconResource);
        abstract Builder icon(final String icon);
        abstract Builder weight(final int weight);
        abstract Builder type(final String type);
        abstract Plugin build();
    }

    @VisibleForTesting
    protected static Plugin create(String cmd, int iconResource) {
        return Plugin.builder()
                .id(cmd)
                .name("")
                .iconResource(iconResource)
                .icon("")
                .weight(0)
                .type("")
                .build();
    }

    @NonNull
    public static Plugin fromMap(@NonNull Map<String, String> record) {
        return Plugin.builder()
                .id(record.get("cmd"))
                .name(record.get("name"))
                .icon(record.get("icon"))
                .iconResource(0)
                .weight(Util.parseDecimalIntOrZero(record.get("weight")))
                .type(Strings.nullToEmpty(record.get("type")))
                .build();
    }

    @Override
    public String intentExtraKey() {
        return Plugin.class.getName();
    }
}
