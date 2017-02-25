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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class MusicFolderItemTest {
    private final Map<String, String> musicFolderItemMap = ImmutableMap.<String, String>builder()
            .put("id", "1")
            .put("filename", "file.mp3")
            .put("type", "track")
            .put("url", "http://some/url")
            .put("download_url", "http://some/download/url")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_Complete() {
        MusicFolderItem musicFolderItem = MusicFolderItem.fromMap(musicFolderItemMap);
        assertEquals("1", musicFolderItem.id());
        assertEquals("file.mp3", musicFolderItem.name());
        assertEquals("track", musicFolderItem.type());
        assertEquals(Uri.parse("http://some/url"), musicFolderItem.url());
        assertEquals(Uri.parse("http://some/download/url"), musicFolderItem.downloadUrl());
        assertEquals("track_id", musicFolderItem.playlistTag());
    }

    @Test public void fromMap_NoId_Throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null id");
        assertNull(MusicFolderItem.fromMap(filterKey(musicFolderItemMap, "id")));
    }

    @Test public void fromMap_NoName_Throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null name");
        assertNull(MusicFolderItem.fromMap(filterKey(musicFolderItemMap, "filename")));
    }

    @Test public void fromMap_NoType_Throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null type");
        assertNull(MusicFolderItem.fromMap(filterKey(musicFolderItemMap, "type")));
    }

    @Test public void fromMap_PlaylistTag_Playlist() {
        Map<String, String> map = new HashMap<>(musicFolderItemMap);
        map.put("type", "playlist");
        MusicFolderItem musicFolderItem = MusicFolderItem.fromMap(map);
        assertEquals("playlist_id", musicFolderItem.playlistTag());
    }

    @Test public void fromMap_PlaylistTag_Folder() {
        Map<String, String> map = new HashMap<>(musicFolderItemMap);
        map.put("type", "folder");
        MusicFolderItem musicFolderItem = MusicFolderItem.fromMap(map);
        assertEquals("folder_id", musicFolderItem.playlistTag());
    }

    @Test public void fromMap_PlaylistTag_Unknown() {
        Map<String, String> map = new HashMap<>(musicFolderItemMap);
        map.put("type", "not-a-valid-type");
        MusicFolderItem musicFolderItem = MusicFolderItem.fromMap(map);
        assertEquals("Unknown_type_in_playlistTag()", musicFolderItem.playlistTag());
    }

    @Test public void fromMap_EmptyUrl_IsEmpty() {
        MusicFolderItem musicFolderItem = MusicFolderItem.fromMap(
                filterKey(musicFolderItemMap, "url"));
        assertEquals(Uri.EMPTY, musicFolderItem.url());
    }

    @Test public void fromMap_EmptyDownloadUrl_IsEmpty() {
        MusicFolderItem musicFolderItem = MusicFolderItem.fromMap(
                filterKey(musicFolderItemMap, "download_url"));
        assertEquals(Uri.EMPTY, musicFolderItem.downloadUrl());
    }

    @Test public void intentExtraKey() {
        MusicFolderItem musicFolderItem = MusicFolderItem.fromMap(musicFolderItemMap);
        assertEquals(MusicFolderItem.class.getName(), musicFolderItem.intentExtraKey());
    }
}