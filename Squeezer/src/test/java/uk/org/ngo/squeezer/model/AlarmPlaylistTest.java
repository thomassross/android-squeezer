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

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class AlarmPlaylistTest {
    private final Map<String, String> alarmPlaylistMap = ImmutableMap.<String, String>builder()
            .put("url", "http://some/playlist")
            .put("title", "Playlist title")
            .put("category", "Playlist category")
            .put("singleton", "1")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_Complete() {
        AlarmPlaylist alarmPlaylist = AlarmPlaylist.fromMap(alarmPlaylistMap);
        assertEquals("http://some/playlist", alarmPlaylist.id());
        assertEquals("Playlist title", alarmPlaylist.name());
        assertEquals("Playlist category", alarmPlaylist.category());
        assertTrue(alarmPlaylist.singleton());
    }

    @Test public void fromMap_NoUrl_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: id");
        assertNull(AlarmPlaylist.fromMap(filterKey(alarmPlaylistMap, "url")));
    }

    @Test public void fromMap_NoTitle_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: name");
        assertNull(AlarmPlaylist.fromMap(filterKey(alarmPlaylistMap, "title")));
    }

    @Test public void fromMap_NoCategory_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: category");
        assertNull(AlarmPlaylist.fromMap(filterKey(alarmPlaylistMap, "category")));
    }

    @Test public void fromMap_NoSingleton_IsFalse() {
        assertFalse(AlarmPlaylist.fromMap(filterKey(alarmPlaylistMap, "singleton")).singleton());
    }
}