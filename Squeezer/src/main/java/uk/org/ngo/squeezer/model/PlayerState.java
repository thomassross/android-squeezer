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


import android.os.Parcelable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.EnumIdLookup;
import uk.org.ngo.squeezer.framework.EnumWithId;
import uk.org.ngo.squeezer.service.ServerString;


@AutoValue
public abstract class PlayerState implements Parcelable {

    @StringDef({NOTIFY_NONE, NOTIFY_ON_CHANGE, NOTIFY_REAL_TIME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerSubscriptionType {}
    public static final String NOTIFY_NONE = "-";
    public static final String NOTIFY_ON_CHANGE = "0";
    public static final String NOTIFY_REAL_TIME = "1";

    /** tag="playerid", unique identifier for the player. */
    public abstract String playerId();

    /**
     * tag="mode", the player's state. May be null, which indicates that Squeezer has received
     * a "players" response for this player, but has not yet received a status message
     * for it.
     */
    @NonNull
    public abstract @PlayState String playStatus();

    /** tag="power", true if the player is on, ignored for remote streaming connections. */
    public abstract boolean poweredOn();

    /** tag="playlist shuffle", shuffle status. Null means unknown. */
    @Nullable
    public abstract ShuffleStatus shuffleStatus();

    /** tag="playlist repeat", Repeat status. Null means unknown. */
    @Nullable
    public abstract RepeatStatus repeatStatus();

    @Nullable
    public abstract Song currentSong();

    /** tag="playlist_name", name of current playlist, or the empty string. */
    @NonNull
    public abstract String currentPlaylist();

    /** tag="playlist_cur_index", position in the playlist of the current song. */
    public abstract int currentPlaylistIndex();

    /** tag="playlist_tracks", number of tracks in the current playlist. */
    public abstract int currentPlaylistTracksNum();

    /** tag="time", elapsed time in to current song, measured in seconds. */
    public abstract int currentTimeSecond();

    /** tag="duration", duration of current song, measured in seconds. */
    // TODO: Get rid of this? It's parsed out from the song as well.
    public abstract int currentSongDuration();

    /** tag="mixer volume", player volume. */
    public abstract int currentVolume();

    /** tag="sleep", if set to sleep, the number of seconds the player sleeps. */
    public abstract int sleepDuration();

    /** tag="will_sleep_in", seconds left until the player sleeps. */
    public abstract int sleep();

    /** tag="sync_master", the id of the player this player is synced to, null if it's not synced. */
    @Nullable
    public abstract String syncMaster();

    /** tag="sync_slaves", IDs of players synced to this player. May be the empty list if no players are synced. */
    // Should be immutable, but the auto-generated code to unparcel tries to assign an
    // ArrayList to this field, which is not compatible.
    @NonNull
    public abstract List<String> syncSlaves();

    /* tag="subscribe", how the server is subscribed to the player's status changes. */
    @Nullable
    public abstract @PlayerSubscriptionType String subscriptionType();

    public boolean isPlaying() {
        return PLAY_STATE_PLAY.equals(playStatus());
    }

    @StringDef({PLAY_STATE_PLAY, PLAY_STATE_PAUSE, PLAY_STATE_STOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayState {}
    public static final String PLAY_STATE_PLAY = "play";
    public static final String PLAY_STATE_PAUSE = "pause";
    public static final String PLAY_STATE_STOP = "stop";
    public static final String PLAY_STATE_UNKNOWN = "UNKNOWN"; // Not a valid player state

    public enum ShuffleStatus implements EnumWithId {
        SHUFFLE_OFF(0, R.attr.ic_action_av_shuffle_off, ServerString.SHUFFLE_OFF),
        SHUFFLE_SONG(1, R.attr.ic_action_av_shuffle_song, ServerString.SHUFFLE_ON_SONGS),
        SHUFFLE_ALBUM(2, R.attr.ic_action_av_shuffle_album, ServerString.SHUFFLE_ON_ALBUMS);

        private final int id;

        private final int icon;

        private final ServerString text;

        private static final EnumIdLookup<ShuffleStatus> lookup = new EnumIdLookup<>(
                ShuffleStatus.class);

        ShuffleStatus(int id, int icon, ServerString text) {
            this.id = id;
            this.icon = icon;
            this.text = text;
        }

        @Override
        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public ServerString getText() {
            return text;
        }

        public static ShuffleStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

    public enum RepeatStatus implements EnumWithId {
        REPEAT_OFF(0, R.attr.ic_action_av_repeat_off, ServerString.REPEAT_OFF),
        REPEAT_ONE(1, R.attr.ic_action_av_repeat_one, ServerString.REPEAT_ONE),
        REPEAT_ALL(2, R.attr.ic_action_av_repeat_all, ServerString.REPEAT_ALL);

        private final int id;

        private final int icon;

        private final ServerString text;

        private static final EnumIdLookup<RepeatStatus> lookup = new EnumIdLookup<>(
                RepeatStatus.class);

        RepeatStatus(int id, int icon, ServerString text) {
            this.id = id;
            this.icon = icon;
            this.text = text;
        }

        @Override
        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public ServerString getText() {
            return text;
        }

        public static RepeatStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

    public static Builder builder() {
        return new AutoValue_PlayerState.Builder()
                .playerId("")
                .playStatus(PLAY_STATE_UNKNOWN)
                .poweredOn(false)
                .currentPlaylist("")
                .currentPlaylistIndex(0)
                .currentPlaylistTracksNum(0)
                .currentTimeSecond(0)
                .currentSongDuration(0)
                .currentVolume(0)
                .sleepDuration(0)
                .sleep(0)
                .syncSlaves(Collections.<String>emptyList())
                .subscriptionType(NOTIFY_NONE);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder playerId(final String playerId);
        public abstract Builder playStatus(final String playStatus);
        public abstract Builder poweredOn(final boolean poweredOn);
        public abstract Builder shuffleStatus(final ShuffleStatus shuffleStatus);
        public abstract Builder repeatStatus(final RepeatStatus repeatStatus);
        public abstract Builder currentSong(final Song currentSong);
        public abstract Builder currentPlaylist(final String currentPlaylist);
        public abstract Builder currentPlaylistIndex(final int currentPlaylistIndex);
        public abstract Builder currentPlaylistTracksNum(final int currentPlaylistTracksNum);
        public abstract Builder currentTimeSecond(final int currentTimeSecond);
        public abstract Builder currentSongDuration(final int currentSongDuration);
        public abstract Builder currentVolume(final int currentVolume);
        public abstract Builder sleepDuration(final int sleepDuration);
        public abstract Builder sleep(final int sleep);
        public abstract Builder syncMaster(final String syncMaster);
        public abstract Builder syncSlaves(final List<String> syncSlaves);
        public abstract Builder subscriptionType(final String subscriptionType);

        abstract String playStatus();
        abstract String subscriptionType();
        abstract PlayerState autoBuild();

        public PlayerState build() {
            if (Strings.isNullOrEmpty(playStatus())) {
                playStatus(PLAY_STATE_UNKNOWN);
            }
            if (Strings.isNullOrEmpty(subscriptionType())) {
                subscriptionType(NOTIFY_NONE);
            }
            return autoBuild();
        }
    }

    public static PlayerState fromMap(@NonNull Map<String, String> record) {
        return PlayerState.builder()
                .playerId(record.get("playerid"))
                .playStatus(record.get("mode"))
                .poweredOn(Util.parseDecimalIntOrZero(record.get("power")) == 1)
                // Previous behaviour was to set this to null if shuffle state was missing.
                // This sets it to 0.  Maybe have an UNKNOWN state, and set it to that instead?
                // This is checked in e.g. NowPlayingFragment.updateShuffleStatus
                .shuffleStatus(ShuffleStatus.valueOf(
                        Util.parseDecimalIntOrZero(record.get("playlist shuffle"))))
                // As above, this also maps null to 0 instead of null.
                .repeatStatus(RepeatStatus.valueOf(
                        Util.parseDecimalIntOrZero(record.get("playlist repeat"))))
                .currentPlaylistTracksNum(Util.parseDecimalIntOrZero(record.get("playlist_tracks")))
                .currentPlaylistIndex(Util.parseDecimalIntOrZero(record.get("playlist_cur_index")))
                .currentPlaylist(Strings.nullToEmpty(record.get("playlist_name")))
                .sleep(Util.parseDecimalIntOrZero(record.get("will_sleep_in")))
                .sleepDuration(Util.parseDecimalIntOrZero(record.get("sleep")))
                // XXX What if this returns null?
                .currentSong(Song.fromMap(record))
                .currentSongDuration(Util.parseDecimalIntOrZero(record.get("duration")))
                .currentTimeSecond(Util.parseDecimalIntOrZero(record.get("time")))
                .currentVolume(Util.parseDecimalIntOrZero(record.get("mixer volume")))
                .syncMaster(record.get("sync_master"))
                .syncSlaves(Splitter.on(",").omitEmptyStrings().splitToList(Strings.nullToEmpty(
                        record.get("sync_slaves")
                )))
                .subscriptionType(record.get("subscribe"))
                .build();
    }

    @CheckResult
    public abstract PlayerState withPlayStatus(@NonNull @PlayState String playStatus);

    @CheckResult
    public abstract PlayerState withCurrentSong(Song currentSong);

    @CheckResult
    public abstract PlayerState withCurrentVolume(int currentVolume);
}
