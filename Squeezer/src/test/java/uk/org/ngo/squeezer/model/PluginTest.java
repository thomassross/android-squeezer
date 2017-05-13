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

public class PluginTest {
    private final Map<String, String> pluginMap = ImmutableMap.<String, String>builder()
            .put("cmd", "myapps")
            .put("name", "Plugin name")
            .put("icon", "plugins/some/icon")
            .put("weight", "1")
            .put("type", "xmlbrowser_search")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCreate() {
        Plugin plugin = Plugin.create("myapps", 1);
        assertEquals("myapps", plugin.id());
        assertEquals("", plugin.name());
        assertEquals(1, plugin.iconResource());
        assertEquals("", plugin.icon());
        assertEquals(0, plugin.weight());
        assertEquals("", plugin.type());
    }

    @Test
    public void fromMap_Complete() {
        Plugin plugin = Plugin.fromMap(pluginMap);
        assertEquals("myapps", plugin.id());
        assertEquals("Plugin name", plugin.name());
        assertEquals("plugins/some/icon", plugin.icon());
        assertEquals(0, plugin.iconResource());
        assertEquals(1, plugin.weight());
        assertEquals("xmlbrowser_search", plugin.type());
    }

    @Test
    public void fromMap_NoId_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: id");
        assertNull(Plugin.fromMap(filterKey(pluginMap, "cmd")));
    }

    @Test
    public void fromMap_NoName_Throws() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Missing required properties: name");
        assertNull(Plugin.fromMap(filterKey(pluginMap, "name")));
    }

    @Test
    public void fromMap_NoIcon_IsNull() {
        assertNull(Plugin.fromMap(filterKey(pluginMap, "icon")).icon());
    }

    @Test
    public void fromMap_NoWeight_Is0() {
        assertEquals(0, Plugin.fromMap(filterKey(pluginMap, "weight")).weight());
    }

    @Test
    public void fromMap_NoType_IsEmpty() {
        assertEquals("", Plugin.fromMap(filterKey(pluginMap, "type")).type());
    }

    @Test
    public void fromMap_TypeXmlBrowserSearch_IsSearchable() {
        assertTrue(Plugin.fromMap(pluginMap).isSearchable());
    }

    @Test
    public void fromMap_TypeOther_IsNotSearchable() {
        assertFalse(Plugin.fromMap(filterKey(pluginMap, "type")).isSearchable());
    }
}
