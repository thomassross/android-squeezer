/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;

import org.jetbrains.annotations.Contract;

import java.util.BitSet;
import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;

@AutoValue
public abstract class Alarm extends Item {

    /** tag="dow", days of the week the alarm is active, 0=Sunday, 6=Saturday. */
    public abstract BitSet dow();

    /** tag="enabled", true if the alarm is enabled. */
    public abstract boolean enabled();

    /** tag="repeat", true if the alarm repeats. */
    public abstract boolean repeat();

    /** tag="time", time of day the alarm fires, in seconds-since-midnight. */
    public abstract int tod();

    /** tag="url", URL of the alarm's playlist, or the string "CURRENT_PLAYLIST". */
    public abstract String url();

    @NonNull
    @Contract(" -> !null")
    private static Builder builder() {
        return new AutoValue_Alarm.Builder();
    }

    abstract Builder toBuilder();

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(final String id);
        abstract Builder name(final String name);
        abstract Builder dow(final BitSet dow);
        abstract Builder enabled(final boolean enabled);
        abstract Builder repeat(final boolean repeat);
        abstract Builder tod(final int tod);
        abstract Builder url(final String url);
        abstract Alarm build();
    }

    @NonNull
    public static Alarm fromMap(@NonNull Map<String, String> record) {
        String url = record.get("url");
        if ("CURRENT_PLAYLIST".equals(url)) {
            url = "";
        }

        return Alarm.builder()
                .id(record.get("id"))
                .name("")  // Alarms have no name
                .dow(dowFromString(record.get("dow")))
                .enabled(Util.parseDecimalIntOrZero(record.get("enabled")) == 1)
                .repeat(Util.parseDecimalIntOrZero(record.get("repeat")) == 1)
                .tod(Util.parseDecimalIntOrZero(record.get("time")))
                .url(url)
                .build();
    }

    /**
     * Construct a bitset from the given string.
     *
     * @param dowString Comma-separated string of numbers in range [0-6].
     */
    @NonNull
    @VisibleForTesting
    static BitSet dowFromString(@NonNull String dowString) {
        BitSet bits = new BitSet(7);

        if ("".equals(dowString)) {
            return bits;
        }

        final Iterable<String> days = Splitter.on(',')
                .trimResults().omitEmptyStrings().split(dowString);

        for (String day : days) {
            int i = Util.parseDecimalIntOrZero(day);
            if (i <= 6) {
                bits.set(i, true);
            }
        }

        return bits;
    }

    public boolean isDayActive(@IntRange(from=0,to=6) int day) {
        if (day < 0 || day > 6) {
            Util.crashlyticsLog(Log.ERROR, "Alarm", "isDayActive(): day out of range: " + day);
            return false;
        }
        return dow().get(day);
    }

    /**
     * Return a copy of the alarm with the additional day set.
     *
     * @param day The day to set, 0=Sunday, 6=Saturday.
     * @return a new Alarm.
     */
    @CheckResult
    public Alarm setDay(@IntRange(from=0,to=6) int day) {
        if (day < 0 || day > 6) {
            Util.crashlyticsLog(Log.ERROR, "Alarm", "isDayActive(): day out of range: " + day);
            return toBuilder().build();
        }
        BitSet bits = dow().get(0, 7);  // Copy, so next .set() doesn't modify this object.
        bits.set(day, true);
        return toBuilder().dow(bits).build();
    }

    /**
     * Return a copy of the alarm with given day cleared.
     *
     * @param day The day to set, 0=Sunday, 6=Saturday.
     * @return a new Alarm.
     */
    @CheckResult
    public Alarm clearDay(@IntRange(from=0,to=6) int day) {
        if (day < 0 || day > 6) {
            Util.crashlyticsLog(Log.ERROR, "Alarm", "isDayActive(): day out of range: " + day);
            return toBuilder().build();
        }
        BitSet bits = dow().get(0, 7);  // Copy, so next .set() doesn't modify this object.
        bits.set(day, false);
        return toBuilder().dow(bits).build();
    }

    @CheckResult
    public abstract Alarm withEnabled(boolean enabled);

    @CheckResult
    public abstract Alarm withRepeat(boolean repeat);

    @CheckResult
    public abstract Alarm withUrl(String url);

    @CheckResult
    public abstract Alarm withTod(int tod);
}
