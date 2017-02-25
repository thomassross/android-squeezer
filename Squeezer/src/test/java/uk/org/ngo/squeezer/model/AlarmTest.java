/*
 * Copyright (c) 2017 Google Inc.  All Rights Reserved.
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

import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.BitSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class AlarmTest {
    private final Map<String, String> alarmMap = ImmutableMap.<String, String>builder()
            .put("id", "1")
            .put("dow", "0,1,2,3,4,5,6")
            .put("enabled", "1")
            .put("repeat", "1")
            .put("time", "3600")
            .put("url", "http://some/playlist")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_CompleteAlarm() {
        Alarm alarm = Alarm.fromMap(alarmMap);
        assertEquals("1", alarm.id());
        assertEquals("", alarm.name());
        assertEquals(BitSet.valueOf(new byte[]{0b1111111}), alarm.dow());
        assertEquals(true, alarm.enabled());
        assertEquals(true, alarm.repeat());
        assertEquals(3600, alarm.tod());
        assertEquals("http://some/playlist", alarm.url());
    }

    /** "id" key is required to construct the object. */
    @Test public void fromMap_NoId_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: id");
        assertNull(Alarm.fromMap(filterKey(alarmMap, "id")).id());
    }

    /** "dow" key is required to construct the object. */
    @Test public void fromMap_NoDow_Throws() {
        thrown.expect(NullPointerException.class);
        assertNull(Alarm.fromMap(filterKey(alarmMap, "dow")).dow());
    }

    @Test public void fromMap_NoEnabled_IsFalse() {
        assertFalse(Alarm.fromMap(filterKey(alarmMap, "enabled")).enabled());
    }

    @Test public void fromMap_NoRepeat_IsFalse() {
        assertFalse(Alarm.fromMap(filterKey(alarmMap, "repeat")).repeat());
    }

    @Test public void fromMap_NoTime_Is0() {
        assertEquals(0, Alarm.fromMap(filterKey(alarmMap, "time")).tod());
    }

    @Test public void fromMap_NoUrl_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: url");
        assertNull(Alarm.fromMap(filterKey(alarmMap, "url")).url());
    }

    @Test public void fromMap_CURRENT_PLAYLIST() {
        Map<String, String> alarmMap = ImmutableMap.<String, String>builder()
                .put("id", "1")
                .put("dow", "0,1,2,3,4,5,6")
                .put("enabled", "1")
                .put("repeat", "1")
                .put("time", "3600")
                .put("url", "CURRENT_PLAYLIST")
                .build();

        assertEquals("", Alarm.fromMap(alarmMap).url());
    }

    @Test public void dowFromString() {
        Map<String, BitSet> tests = ImmutableMap.<String, BitSet>builder()
                // Obvious things that should work.
                .put("", new BitSet(7))
                .put("0", BitSet.valueOf(new byte[]{0b1}))
                .put("0,1", BitSet.valueOf(new byte[]{0b11}))
                .put("0,1,2", BitSet.valueOf(new byte[]{0b111}))
                .put("0,1,2,3", BitSet.valueOf(new byte[]{0b1111}))
                .put("0,1,2,3,4", BitSet.valueOf(new byte[]{0b11111}))
                .put("0,1,2,3,4,5", BitSet.valueOf(new byte[]{0b111111}))
                .put("0,1,2,3,4,5,6", BitSet.valueOf(new byte[]{0b1111111}))
                // Make sure that days can be specified in arbitrary order.
                .put("6,3,0", BitSet.valueOf(new byte[]{0b1001001}))
                // Make sure that whitespace is ignored
                .put("  6,   2,   0", BitSet.valueOf(new byte[]{0b1000101}))
                // Makee sure that extra commas are ignored.
                .put("5,,,,2,,,,", BitSet.valueOf(new byte[]{0b100100}))
                // Make sure that bits > 6 are ignored
                .put("8,6,1", BitSet.valueOf(new byte[]{0b1000010}))
                .build();

        for (Map.Entry<String, BitSet> test : tests.entrySet()) {
            assertEquals(test.getKey(), test.getValue(), Alarm.dowFromString(test.getKey()));
        }
    }

    @Test public void isDayActive() {
        // alarmMap sets all days, so all days should be active.
        Alarm alarm = Alarm.fromMap(alarmMap);
        for (int day = 0; day < 7; day++) {
            assertTrue(alarm.isDayActive(day));
        }
    }

    @Test public void setDay() {
        // First clear all the days and confirm they're clear.
        Alarm clearedAlarm = Alarm.fromMap(alarmMap);
        for (int day = 0; day < 7; day++) {
            clearedAlarm = clearedAlarm.clearDay(day);
        }
        for (int day = 0; day < 7; day++) {
            assertFalse(clearedAlarm.isDayActive(day));
        }

        // Then set them back one by and one and confirm they're set.
        for (int dayToSet = 0; dayToSet < 7; dayToSet++) {
            Alarm alarm = clearedAlarm.setDay(dayToSet);
            for (int day = 0; day < 7; day++) {
                if (dayToSet == day) {
                    assertTrue(alarm.isDayActive(day));
                } else {
                    assertFalse(alarm.isDayActive(day));
                }
            }
        }
    }

    @Test public void setDay_OutOfRange() {
        // Trying to set an invalid day should return an Alarm that
        // is .equals() to the previous one, but that is not the same
        // object.
        Alarm alarm = Alarm.fromMap(alarmMap);
        Alarm newAlarm = alarm.setDay(-1);

        assertTrue(newAlarm.equals(alarm));
        assertFalse(newAlarm == alarm);
    }

    @Test public void clearDay() {
        // Clear days one at a time and check each is cleared.
        for (int dayToClear = 0; dayToClear < 7; dayToClear++) {
            Alarm alarm = Alarm.fromMap(alarmMap).clearDay(dayToClear);
            for (int day = 0; day < 7; day++) {
                if (dayToClear == day) {
                    assertFalse(alarm.isDayActive(day));
                } else {
                    assertTrue(alarm.isDayActive(day));
                }
            }
        }
    }

    @Test public void clearDay_OutOfRange() {
        // Trying to clear an invalid day should return an Alarm that
        // is .equals() to the previous one, but that is not the same
        // object.
        Alarm alarm = Alarm.fromMap(alarmMap);
        Alarm newAlarm = alarm.clearDay(-1);

        assertTrue(newAlarm.equals(alarm));
        assertFalse(newAlarm == alarm);
    }
}
