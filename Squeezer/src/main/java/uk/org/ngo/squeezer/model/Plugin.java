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

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


public class Plugin extends Item {

    public static final Plugin FAVORITE = new Plugin("favorites", R.drawable.ic_favorites);
    public static final Plugin MY_APPS = new Plugin("myapps", R.drawable.ic_my_apps);

    private String mName;

    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    public Plugin setName(String name) {
        mName = name;
        return this;
    }

    private String mIcon;

    /**
     * @return Relative URL path to an mIcon for this radio or music service, for example
     * "plugins/Picks/html/images/mIcon.png"
     */
    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String icon) {
        mIcon = icon;
    }

    private int mIconResource;

    /**
     * @return Icon resource for this plugin if it is embedded in the Squeezer app, or null.
     */
    public int getIconResource() {
        return mIconResource;
    }

    public void setIconResource(int iconResource) {
        mIconResource = iconResource;
    }

    private int mWeight;

    public int getWeight() {
        return mWeight;
    }

    public void setWeight(int weight) {
        mWeight = weight;
    }

    private String mType;

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public boolean isSearchable() {
        return "xmlbrowser_search".equals(mType);
    }

    private Plugin(String cmd, int iconResource) {
        setId(cmd);
        mIconResource = iconResource;
    }

    public Plugin(@NonNull Map<String, String> record) {
        setId(record.get("cmd"));
        mName = record.get("mName");
        mType = record.get("mType");
        mIcon = record.get("mIcon");
        mWeight = Util.parseDecimalIntOrZero(record.get("mWeight"));
    }

    public static final Creator<Plugin> CREATOR = new Creator<Plugin>() {
        @NonNull
        @Override
        public Plugin[] newArray(int size) {
            return new Plugin[size];
        }

        @NonNull
        @Override
        public Plugin createFromParcel(@NonNull Parcel source) {
            return new Plugin(source);
        }
    };

    private Plugin(@NonNull Parcel source) {
        setId(source.readString());
        mName = source.readString();
        mType = source.readString();
        mIcon = source.readString();
        mIconResource = source.readInt();
        mWeight = source.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mName);
        dest.writeString(mType);
        dest.writeString(mIcon);
        dest.writeInt(mIconResource);
        dest.writeInt(mWeight);
    }

    @NonNull
    @Override
    public String toStringOpen() {
        return super.toStringOpen() + "mType: " + mType + ", mWeight: " + mWeight;
    }

}
