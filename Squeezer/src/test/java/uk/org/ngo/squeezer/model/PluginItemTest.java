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

public class PluginItemTest {
    private final Map<String, String> pluginItemMap = ImmutableMap.<String, String>builder()
            .put("id", "1")
            .put("name", "Item name")
            .put("title", "Item title")
            .put("description", "Item description")
            .put("image", "some/url/file.png")
            .put("hasitems", "1")
            .put("type", "audio")
            .put("isaudio", "1")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_Complete() {
        PluginItem pluginItem = PluginItem.fromMap(pluginItemMap);
        assertEquals("1", pluginItem.id());
        assertEquals("Item name", pluginItem.name());
        assertEquals("Item description", pluginItem.description());
        assertEquals("some/url/file.png", pluginItem.image());
        assertTrue(pluginItem.hasitems());
        assertEquals("audio", pluginItem.type());
        assertTrue(pluginItem.isAudio());
    }

    @Test public void fromMap_NoId_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: id");
        assertNull(PluginItem.fromMap(filterKey(pluginItemMap, "id")));
    }

    @Test public void fromMap_NoName_UsesTitle() {
        PluginItem pluginItem = PluginItem.fromMap(filterKey(pluginItemMap, "name"));
        assertEquals("Item title", pluginItem.name());
    }

    @Test public void fromMap_NoNameTitle_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: name");
        assertNull(PluginItem.fromMap(filterKey(filterKey(pluginItemMap, "name"), "title")).name());
    }

    @Test public void fromMap_NoDescription_IsEmpty() {
        assertEquals("", PluginItem.fromMap(filterKey(pluginItemMap, "description")).description());
    }

    @Test public void fromMap_NoImage_IsNull() {
        assertNull(PluginItem.fromMap(filterKey(pluginItemMap, "image")).image());
    }

    @Test public void fromMap_NoHasItems_IsFalse() {
        assertFalse(PluginItem.fromMap(filterKey(pluginItemMap, "hasitems")).hasitems());
    }

    @Test public void fromMap_NoType_IsEmpty() {
        assertEquals("", PluginItem.fromMap(filterKey(pluginItemMap, "type")).type());
    }

    @Test public void fromMap_NoIsAudio_IsFalse() {
        assertFalse(PluginItem.fromMap(filterKey(pluginItemMap, "isaudio")).isAudio());
    }

    @Test public void testIntentExtraKey() {
        assertEquals(PluginItem.class.getName(), PluginItem.fromMap(pluginItemMap).intentExtraKey());
    }
}