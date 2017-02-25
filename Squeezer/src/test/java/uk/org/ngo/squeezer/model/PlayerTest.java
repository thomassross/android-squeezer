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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.org.ngo.squeezer.model.ModelTestUtil.filterKey;

public class PlayerTest {
    private final Map<String, String> playerMap = ImmutableMap.<String, String>builder()
            .put("playerid", "1")
            .put("name", "Test player")
            .put("ip", "127.0.0.1")
            .put("model", "Test model")
            .put("canpoweroff", "1")
            .put("connected", "1")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_Complete() {
        Player player = Player.fromMap(playerMap);
        assertEquals("1", player.id());
        assertEquals("Test player", player.name());
        assertEquals("127.0.0.1", player.ip());
        assertEquals("Test model", player.model());
        assertTrue(player.canPowerOff());
        assertTrue(player.connected());
        assertNotNull(player.playerState());
    }

    @Test public void fromMap_NoId_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: playerId");
        assertNull(Player.fromMap(filterKey(playerMap, "playerid")));
    }

    @Test public void fromMap_NoName_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: name");
        assertNull(Player.fromMap(filterKey(playerMap, "name")));
    }

    @Test public void fromMap_NoIp_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: ip");
        assertNull(Player.fromMap(filterKey(playerMap, "ip")));
    }

    @Test public void fromMap_NoModel_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: model");
        assertNull(Player.fromMap(filterKey(playerMap, "model")));
    }

    @Test public void fromMap_NoCanPowerOff_IsFalse() {
        assertFalse(Player.fromMap(filterKey(playerMap, "canpoweroff")).canPowerOff());
    }

    @Test public void fromMap_NoConnected_IsFalse() {
        assertFalse(Player.fromMap(filterKey(playerMap, "connected")).connected());
    }
}