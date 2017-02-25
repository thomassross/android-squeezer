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

import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import org.jetbrains.annotations.Contract;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


@AutoValue
public abstract class Player extends Item implements Comparable, Parcelable {

    /** tag="ip", player IP and port number. */
    @NonNull
    public abstract String ip();

    /** tag="model", player model name. */
    @NonNull
    public abstract String model();

    /** tag="canpoweroff", true if the player can be powered off. */
    public abstract boolean canPowerOff();

    /** The player's current state. */
    @NonNull
    public abstract PlayerState playerState();

    /** tag="connected", true if the player is connected to the server. */
    public abstract boolean connected();

    @Memoized
    public long idAsLong() {
        HashFunction hf = Hashing.goodFastHash(64);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return hf.hashString(id(), Charsets.UTF_8).asLong();
        }

        // API versions < GINGERBREAD do not have String.getBytes(Charset charset),
        // which hashString() ends up calling. This will trigger an exception.
        byte[] bytes;
        try {
            bytes = id().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Can't happen, Android's native charset is UTF-8. But just in case
            // we're running on something wacky, fallback to the un-parsed bytes.
            bytes = id().getBytes();
        }
        return hf.hashBytes(bytes).asInt();
    }

    @Override
    public int compareTo(@NonNull Object another) {
        return name().compareToIgnoreCase(((Player) another).name());
    }

    public static class Pref {
        /** The types of player preferences. */
        @StringDef({ALARM_DEFAULT_VOLUME, ALARM_FADE_SECONDS, ALARM_SNOOZE_SECONDS, ALARM_TIMEOUT_SECONDS,
                Pref.ALARMS_ENABLED})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Name {}
        public static final String ALARM_DEFAULT_VOLUME = "alarmDefaultVolume";
        public static final String ALARM_FADE_SECONDS = "alarmfadeseconds";
        public static final String ALARM_SNOOZE_SECONDS = "alarmSnoozeSeconds";
        public static final String ALARM_TIMEOUT_SECONDS = "alarmTimeoutSeconds";
        public static final String ALARMS_ENABLED = "alarmsEnabled";
        public static final Set<String> VALID_PLAYER_PREFS = Sets.newHashSet(
                ALARM_DEFAULT_VOLUME, ALARM_FADE_SECONDS, ALARM_SNOOZE_SECONDS, ALARM_TIMEOUT_SECONDS,
                ALARMS_ENABLED);
    }

    private static Builder builder() {
        return new AutoValue_Player.Builder()
                // playerState can not be null, so ensure it's initialised if not provided.
                .playerState(PlayerState.builder().build());
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(final String id);
        abstract Builder name(final String name);
        abstract Builder ip(final String ip);
        abstract Builder model(final String model);
        abstract Builder canPowerOff(final boolean canPowerOff);
        abstract Builder connected(final boolean connected);
        abstract Builder playerState(final PlayerState playerState);
        abstract Player build();
    }

    @Contract("_ -> !null")
    public static Player fromMap(@NonNull Map<String, String> record) {
        return Player.builder()
                .id(record.get("playerid"))
                .name(record.get("name"))
                .ip(record.get("ip"))
                .model(record.get("model"))
                .canPowerOff(Util.parseDecimalIntOrZero(record.get("canpoweroff")) == 1)
                .connected(Util.parseDecimalIntOrZero(record.get("connected")) == 1)
                .playerState(PlayerState.builder().playerId(record.get("playerid")).build())
                .build();
    }

    @CheckResult
    public abstract Player withName(String name);

    @CheckResult
    public abstract Player withPlayerState(PlayerState playerState);

    /** Comparator to compare two players by ID. */
    public static final Comparator<Player> compareById = new Comparator<Player>() {
        @Override
        public int compare(@NonNull Player lhs, @NonNull Player rhs) {
            return lhs.id().compareTo(rhs.id());
        }
    };
}
