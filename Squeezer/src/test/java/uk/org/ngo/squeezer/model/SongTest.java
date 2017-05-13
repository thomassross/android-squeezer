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
import com.google.common.testing.EqualsTester;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class SongTest {
    /**
     * Verify that the equals() method compares correctly against nulls, other item types, and is
     * reflexive (a = a), symmetric (a = b && b = a), and transitive (a = b && b = c && a = c).
     */
    @Test
    public void testEquals() {
        Map<String, String> record1 = ImmutableMap.of(
                "id", "1", "name", "Song name", "album", "The album", "artist", "The artist");

        new EqualsTester()
                .addEqualityGroup(Song.fromMap(record1), Song.fromMap(record1))
                .addEqualityGroup(Album.fromMap(record1), Album.fromMap(record1))
                .testEquals();
    }

    private static final Map<String, String> albumMap = ImmutableMap.<String, String>builder()
            .put("album_id", "1")
            .put("album", "Album name")
            .build();

    // Also used in PlayerStateTest.
    static final Map<String, String> songMap = ImmutableMap.<String, String>builder()
            .put("track_id", "1")
            .put("track", "track name")
            .put("artist", "artist name")
            .put("compilation", "1")
            .put("coverart", "1")
            .put("duration", "300")
            .put("year", "2000")
            .put("remote", "1")
            .put("tracknum", "4")
            .put("url", "http://www.example.com")
            .put("artwork_url", "http://www.example.com/file.jpg")
            .put("download_url", "http://www.example.com/file.mp3")
            .putAll(albumMap)
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_CompleteSong() {
        Song song = Song.fromMap(songMap);
        assertEquals("1", song.id());
        assertEquals("track name", song.name());
        assertEquals("artist name", song.artist());
        assertTrue(song.compilation());
        assertTrue(song.coverart());
        assertEquals(300, song.duration());
        assertEquals(2000, song.year());
        assertTrue(song.remote());
        assertEquals(4, song.trackNum());
        assertEquals(Uri.parse("http://www.example.com"), song.url());
        assertEquals(Uri.parse("http://www.example.com/file.jpg"), song.artworkUrl());
        assertEquals(Uri.parse("http://www.example.com/file.mp3"), song.downloadUrl());

        assertTrue(song.hasArtwork());

        // Check album info.
        assertEquals("1", song.albumId());
        assertEquals("Album name", song.albumName());
        assertEquals("1", song.album().id());
        assertEquals("Album name", song.album().name());
    }

    @Test public void fromMap_NoArtist() {
        assertNull(Song.fromMap(filterKey(songMap, "artist")).artist());
    }

    @Test public void fromMap_NoAlbum() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: id");
        assertNull(Song.fromMap(filterKey(songMap, "album_id")));
    }

    @Test public void fromMap_NoCompilation_IsFalse() {
        assertFalse(Song.fromMap(filterKey(songMap, "compilation")).compilation());
    }

    @Test public void fromMap_NoCoverArt_IsFalse() {
        assertFalse(Song.fromMap(filterKey(songMap, "coverart")).coverart());
    }

    @Test public void fromMap_NoDuration_Is0() {
        assertEquals(0, Song.fromMap(filterKey(songMap, "duration")).duration());
    }

    @Test public void fromMap_NoYear_Is0() {
        assertEquals(0, Song.fromMap(filterKey(songMap, "year")).year());
    }

    @Test public void fromMap_NoRemote_IsFalse() {
        assertFalse(Song.fromMap(filterKey(songMap, "remote")).remote());
    }

    @Test public void fromMap_NoTrackNum_Is1() {
        assertEquals(1, Song.fromMap(filterKey(songMap, "tracknum")).trackNum());
    }

    @Test public void fromMap_NoUrl_IsEmpty() {
        assertEquals(Uri.EMPTY, Song.fromMap(filterKey(songMap, "url")).url());
    }

    @Test public void fromMap_NoArtworkUrl_IsEmpty() {
        assertEquals(Uri.EMPTY, Song.fromMap(filterKey(songMap, "artwork_url")).artworkUrl());
        assertFalse(Song.fromMap(filterKey(songMap, "artwork_url")).hasArtwork());
    }

    @Test public void fromMap_NoDownloadUrl_IsEmpty() {
        assertEquals(Uri.EMPTY, Song.fromMap(filterKey(songMap, "download_url")).downloadUrl());
    }

    @Test public void fromMap_NoButtons_IsEmpty() {
        assertEquals("", Song.fromMap(filterKey(songMap, "buttons")).buttons());
    }
}
