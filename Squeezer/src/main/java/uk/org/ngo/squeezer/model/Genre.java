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

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.Map;

import uk.org.ngo.squeezer.framework.PlaylistItem;

@AutoValue
public abstract class Genre extends PlaylistItem implements Parcelable {
    @NonNull
    @Override
    public String playlistTag() {
        return "genre_id";
    }

    @NonNull
    @Override
    public String filterTag() {
        return "genre_id";
    }

    public static Genre fromMap(@NonNull Map<String, String> record) {
        return new AutoValue_Genre(record.containsKey("genre_id") ? record.get("genre_id") : record.get("id"),
                record.get("genre"));
    }
}
