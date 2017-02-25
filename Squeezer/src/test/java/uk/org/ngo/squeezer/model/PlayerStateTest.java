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

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class PlayerStateTest {
    private final Map<String, String> playerStateMap = ImmutableMap.<String, String>builder()
            .put("playerid", "player1")
            .put("mode", PlayerState.PLAY_STATE_PLAY)
            .put("power", "1")
            .put("playlist shuffle", String.valueOf(PlayerState.ShuffleStatus.SHUFFLE_ALBUM.getId()))
            .put("playlist repeat", String.valueOf(PlayerState.RepeatStatus.REPEAT_ONE.getId()))
            .put("playlist_tracks", "10")
            .put("playlist_cur_index", "2")
            .put("playlist_name", "Test playlist")
            .put("will_sleep_in", "20")
            .put("sleep", "15")
            //.put("duration", "300")
            .put("time", "15")
            .put("mixer volume", "80")
            .put("sync_master", "player2")
            .put("sync_slaves", "player3,player4")
            .put("subscribe", PlayerState.NOTIFY_NONE)
            .putAll(SongTest.songMap)
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_Complete() {
        PlayerState playerState = PlayerState.fromMap(playerStateMap);
        assertEquals("player1", playerState.playerId());
        assertEquals(PlayerState.PLAY_STATE_PLAY, playerState.playStatus());
        assertTrue(playerState.poweredOn());
        assertEquals(PlayerState.ShuffleStatus.SHUFFLE_ALBUM, playerState.shuffleStatus());
        assertEquals(PlayerState.RepeatStatus.REPEAT_ONE, playerState.repeatStatus());
        assertEquals(10, playerState.currentPlaylistTracksNum());
        assertEquals(2, playerState.currentPlaylistIndex());
        assertEquals("Test playlist", playerState.currentPlaylist());
        assertEquals(20, playerState.sleep());
        assertEquals(15, playerState.sleepDuration());
        assertEquals(300, playerState.currentSongDuration());
        assertEquals(15, playerState.currentTimeSecond());
        assertEquals(80, playerState.currentVolume());
        assertEquals("player2", playerState.syncMaster());
        assertEquals(Arrays.asList("player3", "player4"), playerState.syncSlaves());
        assertEquals(PlayerState.NOTIFY_NONE, playerState.subscriptionType());
        assertEquals(Song.fromMap(SongTest.songMap), playerState.currentSong());
    }

    @Test public void fromMap_NoId_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: playerId");
        assertNull(PlayerState.fromMap(filterKey(playerStateMap, "playerid")));
    }

    @Test public void fromMap_NoMode_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Property \"playStatus\" has not been set");
        assertNull(PlayerState.fromMap(filterKey(playerStateMap, "mode")));
    }

    @Test public void fromMap_NoPower_IsFalse() {
        assertFalse(PlayerState.fromMap(filterKey(playerStateMap, "power")).poweredOn());
    }

    @Test public void fromMap_NoShuffle_IsOff() {
        assertEquals(PlayerState.ShuffleStatus.SHUFFLE_OFF,
                PlayerState.fromMap(filterKey(playerStateMap, "playlist shuffle")).shuffleStatus());
    }

    @Test public void fromMap_NoRepeat_IsOff() {
        assertEquals(PlayerState.RepeatStatus.REPEAT_OFF,
                PlayerState.fromMap(filterKey(playerStateMap, "playlist repeat")).repeatStatus());
    }

    @Test public void fromMap_NoTracks_Is0() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "playlist_tracks")).currentPlaylistTracksNum());
    }

    @Test public void fromMap_NoIndex_Is0() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "playlist_cur_index")).currentPlaylistIndex());
    }

    @Test public void fromMap_NoName_IsEmpty() {
        assertEquals("", PlayerState.fromMap(filterKey(playerStateMap, "playlist_name")).currentPlaylist());
    }

    @Test public void fromMap_NoWillSleepIn_Is0() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "will_sleep_in")).sleep());
    }

    @Test public void fromMap_NoSleep_Is0() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "sleep")).sleepDuration());
    }

    @Test public void fromMap_NoDuration_Is0() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "duration")).currentSongDuration());
    }

    @Test public void fromMap_NoTime_Is0() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "time")).currentTimeSecond());
    }

    @Test public void fromMap_NoVolume_Is0() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "mixer volume")).currentVolume());
    }

    @Test public void fromMap_NoSyncMaster_IsNull() {
        assertNull(PlayerState.fromMap(filterKey(playerStateMap, "sync_master")).syncMaster());
    }

    @Test public void fromMap_NoSyncSlaves_IsEmpty() {
        assertEquals(0, PlayerState.fromMap(filterKey(playerStateMap, "sync_slaves")).syncSlaves().size());
    }

    @Test public void fromMap_NoSubscribe_IsNotifyNone() {
        assertEquals(PlayerState.NOTIFY_NONE,
                PlayerState.fromMap(filterKey(playerStateMap, "subscribe")).subscriptionType());
    }
}