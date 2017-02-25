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

import android.net.Uri;

import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class AlbumTest {
    private final Map<String, String> albumMap = ImmutableMap.<String, String>builder()
            .put("album_id", "1")
            .put("id", "2")
            .put("album", "Album title")
            .put("artist", "Artist name")
            .put("compilation", "1")
            .put("year", "2000")
            .put("artwork_url", "http://some/artwork")
            .put("artwork_track_id", "2")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_Complete() {
        Album album = Album.fromMap(albumMap);
        assertEquals("1", album.id());
        assertEquals("Album title", album.name());
        assertEquals("Various", album.artist());
        assertEquals(2000, album.year());
        assertEquals(Uri.parse("http://some/artwork"), album.artworkUrl());
        assertEquals("2", album.artworkTrackId());
    }

    /** When "album_id" is missing it should fall back to "id". */
    @Test public void fromMap_NoAlbumId_UsesId() {
        Album album = Album.fromMap(filterKey(albumMap, "album_id"));
        assertEquals("2", album.id());
    }

    @Test public void fromMap_NoId_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: id");
        assertNull(Album.fromMap(
                filterKey(filterKey(albumMap, "id"), "album_id")));
    }

    @Test public void fromMap_NoName_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: name");
        assertNull(Album.fromMap(filterKey(albumMap, "album")));
    }

    /** When compilation is not set the artist name from the map should be used. */
    @Test public void fromMap_NoCompilation_UsesArtistName() {
        Album album = Album.fromMap(filterKey(albumMap, "compilation"));
        assertEquals("Artist name", album.artist());
    }

    @Test public void fromMap_NoArtist_IsNull() {
        Album album = Album.fromMap(
                filterKey(filterKey(albumMap, "compilation"), "artist"));
        assertNull(album.artist());
    }

    @Test public void fromMap_NoYear_Is0() {
        Album album = Album.fromMap(filterKey(albumMap, "year"));
        assertEquals(0, album.year());
    }

    @Test public void fromMap_NoArtworkUrl_IsEmpty() {
        Album album = Album.fromMap(filterKey(albumMap, "artwork_url"));
        assertEquals(Uri.EMPTY, album.artworkUrl());
    }

    @Test public void fromMap_NoArtworkTrackId_IsEmpty() {
        Album album = Album.fromMap(filterKey(albumMap, "artwork_track_id"));
        assertEquals("", album.artworkTrackId());
    }

    @Test public void testIntentExtraKey() {
        Album album = Album.fromMap(albumMap);
        assertEquals(Album.class.getName(), album.intentExtraKey());
    }
}