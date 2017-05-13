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

public class ArtistTest {
    private final Map<String, String> artistMap = ImmutableMap.<String, String>builder()
            .put("contributor_id", "1")
            .put("id", "2")
            .put("contributor", "Contributor name")
            .put("artist", "Artist name")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void create() {
        Artist artist = Artist.create("1", "Artist name");
        assertEquals("1", artist.id());
        assertEquals("Artist name", artist.name());
    }

    @Test public void fromMap_Complete() {
        Artist artist = Artist.fromMap(artistMap);
        assertEquals("1", artist.id());
        assertEquals("Contributor name", artist.name());
    }

    @Test public void fromMap_NoContributorId_UsesId() {
        Artist artist = Artist.fromMap(filterKey(artistMap, "contributor_id"));
        assertEquals("2", artist.id());
    }

    @Test public void fromMap_NoId_Throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null id");
        assertNull(Artist.fromMap(filterKey(filterKey(artistMap, "contributor_id"), "id")));
    }

    @Test public void fromMap_NoContributor_UsesArtist() {
        Artist artist = Artist.fromMap(filterKey(artistMap, "contributor"));
        assertEquals("Artist name", artist.name());
    }

    @Test public void fromMap_Noname_Throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null name");
        assertNull(Artist.fromMap(filterKey(filterKey(artistMap, "contributor"), "artist")));
    }
}