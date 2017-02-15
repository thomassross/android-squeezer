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

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;

/**
 * Represents a single item in a plugin.
 */
public class PluginItem extends Item {

    private String mName;

    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    public PluginItem setName(String name) {
        mName = name;
        return this;
    }

    private String mDescription;

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    /**
     * Relative URL to the icon to use for this item.
     */
    private String mImage;

    /**
     * @return the absolute URL to the icon to use for this item
     */
    public String getImage() {
        return mImage;
    }

    public void setImage(String image) {
        mImage = image;
    }

    private boolean mHasitems;

    public boolean isHasitems() {
        return mHasitems;
    }

    public void setHasitems(boolean hasitems) {
        mHasitems = hasitems;
    }

    private String mType;

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    private boolean mAudio;

    public boolean isAudio() {
        return mAudio;
    }

    public void setAudio(boolean audio) {
        mAudio = audio;
    }

    public PluginItem(@NonNull Map<String, String> record) {
        setId(record.get("id"));
        mName = record.containsKey("mName") ? record.get("mName") : record.get("title");
        mDescription = record.get("description");
        mType = record.get("mType");
        mImage = record.get("image");
        mHasitems = (Util.parseDecimalIntOrZero(record.get("mHasitems")) != 0);
        mAudio = (Util.parseDecimalIntOrZero(record.get("isaudio")) != 0);
    }

    public static final Creator<PluginItem> CREATOR = new Creator<PluginItem>() {
        @NonNull
        @Override
        public PluginItem[] newArray(int size) {
            return new PluginItem[size];
        }

        @NonNull
        @Override
        public PluginItem createFromParcel(@NonNull Parcel source) {
            return new PluginItem(source);
        }
    };

    private PluginItem(@NonNull Parcel source) {
        setId(source.readString());
        mName = source.readString();
        mDescription = source.readString();
        mType = source.readString();
        mImage = source.readString();
        mHasitems = (source.readInt() != 0);
        mAudio = (source.readInt() != 0);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mName);
        dest.writeString(mDescription);
        dest.writeString(mType);
        dest.writeString(mImage);
        dest.writeInt(mHasitems ? 1 : 0);
        dest.writeInt(mAudio ? 1 : 0);
    }

    @NonNull
    @Override
    public String toStringOpen() {
        return super.toStringOpen() + ", mType: " + mType + ", hasItems: " + mHasitems +
                ", mAudio: " + mAudio + ", mDescription: " + mDescription;
    }
}
