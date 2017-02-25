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
import static org.junit.Assert.assertNull;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class PlaylistTest {
    private final Map<String, String> playlistMap = ImmutableMap.of(
            "playlist_id", "1", "id", "2", "playlist", "Test playlist");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_PlaylistId() {
        Playlist playlist = Playlist.fromMap(playlistMap);
        assertEquals("1", playlist.id());
        assertEquals("Test playlist", playlist.name());
    }

    /** If "playlist_id" is missing, fall back to using "id". */
    @Test public void fromMap_Id() {
        Playlist playlist = Playlist.fromMap(filterKey(playlistMap, "playlist_id"));
        assertEquals("2", playlist.id());
        assertEquals("Test playlist", playlist.name());
    }

    /** One of "playlist_id" or "id" is required to construct the object. */
    @Test public void fromMap_NoPlaylistIdOrId() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: id");
        assertNull(Playlist.fromMap(ImmutableMap.of("playlist", "Test playlist")));
    }

    /** "playlist" is required to construct the object. */
    @Test public void fromMap_NoPlaylist() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: name");
        assertNull(Playlist.fromMap(filterKey(playlistMap, "playlist")));
    }
}
