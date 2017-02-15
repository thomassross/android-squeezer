/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.EnumIdLookup;
import uk.org.ngo.squeezer.framework.EnumWithId;
import uk.org.ngo.squeezer.service.ServerString;


public class PlayerState implements Parcelable {

    @StringDef({NOTIFY_NONE, NOTIFY_ON_CHANGE, NOTIFY_REAL_TIME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerSubscriptionType {}
    public static final String NOTIFY_NONE = "-";
    public static final String NOTIFY_ON_CHANGE = "0";
    public static final String NOTIFY_REAL_TIME = "1";

    public PlayerState() {
    }

    public static final Creator<PlayerState> CREATOR = new Creator<PlayerState>() {
        @NonNull
        @Override
        public PlayerState[] newArray(int size) {
            return new PlayerState[size];
        }

        @NonNull
        @Override
        public PlayerState createFromParcel(@NonNull Parcel source) {
            return new PlayerState(source);
        }
    };

    private PlayerState(@NonNull Parcel source) {
        mPlayerId = source.readString();
        mPlayStatus = source.readString();
        mPoweredOn = (source.readByte() == 1);
        mShuffleStatus = ShuffleStatus.valueOf(source.readInt());
        repeatStatus = RepeatStatus.valueOf(source.readInt());
        mCurrentSong = source.readParcelable(Song.class.getClassLoader());
        mCurrentPlaylist = source.readString();
        mCurrentPlaylistIndex = source.readInt();
        mCurrentTimeSecond = source.readInt();
        mCurrentSongDuration = source.readInt();
        mCurrentVolume = source.readInt();
        mSleepDuration = source.readInt();
        mSleep = source.readInt();
        mSyncMaster = source.readString();
        source.readStringList(mSyncSlaves);
        mPlayerSubscriptionType = source.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mPlayerId);
        dest.writeString(mPlayStatus);
        dest.writeByte(mPoweredOn ? (byte) 1 : (byte) 0);
        dest.writeInt(mShuffleStatus.getId());
        dest.writeInt(repeatStatus.getId());
        dest.writeParcelable(mCurrentSong, 0);
        dest.writeString(mCurrentPlaylist);
        dest.writeInt(mCurrentPlaylistIndex);
        dest.writeInt(mCurrentTimeSecond);
        dest.writeInt(mCurrentSongDuration);
        dest.writeInt(mCurrentVolume);
        dest.writeInt(mSleepDuration);
        dest.writeInt(mSleep);
        dest.writeString(mSyncMaster);
        dest.writeStringList(mSyncSlaves);
        dest.writeString(mPlayerSubscriptionType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private String mPlayerId;

    private boolean mPoweredOn;

    private @PlayState String mPlayStatus;

    private ShuffleStatus mShuffleStatus;

    private RepeatStatus repeatStatus;

    private Song mCurrentSong;

    /** The name of the current playlist, which may be the empty string. */
    @NonNull
    private String mCurrentPlaylist = "";

    private int mCurrentPlaylistTracksNum;

    private int mCurrentPlaylistIndex;

    private int mCurrentTimeSecond;

    private int mCurrentSongDuration;

    private int mCurrentVolume;

    private int mSleepDuration;

    private int mSleep;

    /** The player this player is synced to (null if none). */
    @Nullable
    private String mSyncMaster;

    /** The players synced to this player. */
    private ImmutableList<String> mSyncSlaves = new ImmutableList.Builder<String>().build();

    /** How the server is subscribed to the player's status changes. */
    @NonNull
    @PlayerSubscriptionType private String mPlayerSubscriptionType = NOTIFY_NONE;

    public boolean isPlaying() {
        return PLAY_STATE_PLAY.equals(mPlayStatus);
    }

    /**
     * @return the player's state. May be null, which indicates that Squeezer has received
     *     a "players" response for this player, but has not yet received a status message
     *     for it.
     */
    @Nullable
    @PlayState
    public String getPlayStatus() {
        return mPlayStatus;
    }

    public boolean setPlayStatus(@NonNull @PlayState String s) {
        if (s.equals(mPlayStatus)) {
            return false;
        }

        mPlayStatus = s;

        return true;
    }

    public String getPlayerId() {
        return mPlayerId;
    }

    public void setPlayerId(String playerId) {
        mPlayerId = playerId;
    }

    public boolean getPoweredOn() {
        return mPoweredOn;
    }

    public boolean isPoweredOn() {
        return mPoweredOn;
    }

    public boolean setPoweredOn(boolean state) {
        if (state == mPoweredOn)
            return false;

        mPoweredOn = state;
        return true;
    }

    public ShuffleStatus getShuffleStatus() {
        return mShuffleStatus;
    }

    public boolean setShuffleStatus(ShuffleStatus status) {
        if (status == mShuffleStatus)
            return false;

        mShuffleStatus = status;
        return true;
    }

    public boolean setShuffleStatus(@Nullable String s) {
        return setShuffleStatus(s != null ? ShuffleStatus.valueOf(Util.parseDecimalIntOrZero(s)) : null);
    }

    public RepeatStatus getRepeatStatus() {
        return repeatStatus;
    }

    public boolean setRepeatStatus(RepeatStatus status) {
        if (status == repeatStatus)
            return false;

        repeatStatus = status;
        return true;
    }

    public boolean setRepeatStatus(@Nullable String s) {
        return setRepeatStatus(s != null ? RepeatStatus.valueOf(Util.parseDecimalIntOrZero(s)) : null);
    }

    public Song getCurrentSong() {
        return mCurrentSong;
    }

    @NonNull
    public String getCurrentSongName() {
        return (mCurrentSong != null) ? mCurrentSong.getName() : "";
    }

    public boolean setCurrentSong(@NonNull Song song) {
        if (song.equals(mCurrentSong))
            return false;

        mCurrentSong = song;
        return true;
    }

    /** @return the name of the current playlist, may be the empty string. */
    @NonNull
    public String getCurrentPlaylist() {
        return mCurrentPlaylist;
    }

    /** @return the number of tracks in the current playlist */
    public int getCurrentPlaylistTracksNum() {
        return mCurrentPlaylistTracksNum;
    }

    public int getCurrentPlaylistIndex() {
        return mCurrentPlaylistIndex;
    }

    public boolean setCurrentPlaylist(@Nullable String playlist) {
        if (playlist == null)
            playlist = "";

        if (playlist.equals(mCurrentPlaylist))
            return false;

        mCurrentPlaylist = playlist;
        return true;
    }

    // set the number of tracks in the current playlist
    public boolean setCurrentPlaylistTracksNum(int value) {
        if (value == mCurrentPlaylistTracksNum)
            return false;

        mCurrentPlaylistTracksNum = value;
        return true;
    }

    public boolean setCurrentPlaylistIndex(int value) {
        if (value == mCurrentPlaylistIndex)
            return false;

        mCurrentPlaylistIndex = value;
        return true;
    }

    public int getCurrentTimeSecond() {
        return mCurrentTimeSecond;
    }

    public boolean setCurrentTimeSecond(int value) {
        if (value == mCurrentTimeSecond)
            return false;

        mCurrentTimeSecond = value;
        return true;
    }

    public int getCurrentSongDuration() {
        return mCurrentSongDuration;
    }

    public boolean setCurrentSongDuration(int value) {
        if (value == mCurrentSongDuration)
            return false;

        mCurrentSongDuration = value;
        return true;
    }

    public int getCurrentVolume() {
        return mCurrentVolume;
    }

    public boolean setCurrentVolume(int value) {
        if (value == mCurrentVolume)
            return false;

        mCurrentVolume = value;
        return true;
    }

    public int getSleepDuration() {
        return mSleepDuration;
    }

    public boolean setSleepDuration(int sleepDuration) {
        if (sleepDuration == mSleepDuration)
            return false;

        mSleepDuration = sleepDuration;
        return true;
    }

    /** @return seconds left until the player sleeps. */
    public int getSleep() {
        return mSleep;
    }

    /**
     *
     * @param sleep seconds left until the player sleeps.
     * @return True if the mSleep value was changed, false otherwise.
     */
    public boolean setSleep(int sleep) {
        if (sleep == mSleep)
            return false;

        mSleep = sleep;
        return true;
    }

    public boolean setSyncMaster(@Nullable String syncMaster) {
        if (syncMaster == null && mSyncMaster == null)
            return false;

        if (syncMaster != null) {
            if (syncMaster.equals(mSyncMaster))
                return false;
        }

        mSyncMaster = syncMaster;
        return true;
    }

    @Nullable
    public String getSyncMaster() {
        return mSyncMaster;
    }

    public boolean setSyncSlaves(@NonNull List<String> syncSlaves) {
        if (syncSlaves.equals(mSyncSlaves))
            return false;

        mSyncSlaves = ImmutableList.copyOf(syncSlaves);
        return true;
    }

    public ImmutableList<String> getSyncSlaves() {
        return mSyncSlaves;
    }

    @NonNull
    @PlayerSubscriptionType public String getSubscriptionType() {
        return mPlayerSubscriptionType;
    }

    public boolean setSubscriptionType(@Nullable @PlayerSubscriptionType String type) {
        if (Strings.isNullOrEmpty(type))
            return setSubscriptionType(NOTIFY_NONE);

        mPlayerSubscriptionType = type;
        return true;
    }

    @StringDef({PLAY_STATE_PLAY, PLAY_STATE_PAUSE, PLAY_STATE_STOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayState {}
    public static final String PLAY_STATE_PLAY = "play";
    public static final String PLAY_STATE_PAUSE = "pause";
    public static final String PLAY_STATE_STOP = "stop";

    public enum ShuffleStatus implements EnumWithId {
        SHUFFLE_OFF(0, R.attr.ic_action_av_shuffle_off, ServerString.SHUFFLE_OFF),
        SHUFFLE_SONG(1, R.attr.ic_action_av_shuffle_song, ServerString.SHUFFLE_ON_SONGS),
        SHUFFLE_ALBUM(2, R.attr.ic_action_av_shuffle_album, ServerString.SHUFFLE_ON_ALBUMS);

        private final int mId;

        private final int mIcon;

        private final ServerString mText;

        private static final EnumIdLookup<ShuffleStatus> lookup = new EnumIdLookup<ShuffleStatus>(
                ShuffleStatus.class);

        ShuffleStatus(int id, int icon, ServerString text) {
            mId = id;
            mIcon = icon;
            mText = text;
        }

        @Override
        public int getId() {
            return mId;
        }

        public int getIcon() {
            return mIcon;
        }

        public ServerString getText() {
            return mText;
        }

        public static ShuffleStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

    public enum RepeatStatus implements EnumWithId {
        REPEAT_OFF(0, R.attr.ic_action_av_repeat_off, ServerString.REPEAT_OFF),
        REPEAT_ONE(1, R.attr.ic_action_av_repeat_one, ServerString.REPEAT_ONE),
        REPEAT_ALL(2, R.attr.ic_action_av_repeat_all, ServerString.REPEAT_ALL);

        private final int mId;

        private final int mIcon;

        private final ServerString mText;

        private static final EnumIdLookup<RepeatStatus> lookup = new EnumIdLookup<RepeatStatus>(
                RepeatStatus.class);

        RepeatStatus(int id, int icon, ServerString text) {
            mId = id;
            mIcon = icon;
            mText = text;
        }

        @Override
        public int getId() {
            return mId;
        }

        public int getIcon() {
            return mIcon;
        }

        public ServerString getText() {
            return mText;
        }

        public static RepeatStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

}
