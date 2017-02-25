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

public class GenreTest {
    private final Map<String, String> genreMap = ImmutableMap.of(
            "genre_id", "1", "id", "2", "genre", "Rock");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test public void fromMap_GenreId() {
        Genre genre = Genre.fromMap(genreMap);
        assertEquals("1", genre.id());
        assertEquals("Rock", genre.name());
    }

    /** If "genre_id" is missing, fall back to using "id". */
    @Test public void fromMap_Id() {
        Genre genre = Genre.fromMap(filterKey(genreMap, "genre_id"));
        assertEquals("2", genre.id());
        assertEquals("Rock", genre.name());
    }

    /** One of "genre_id" or "id" is required to construct the object. */
    @Test public void fromMap_NoGenreIdOrId() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null id");
        assertNull(Genre.fromMap(ImmutableMap.of("genre", "Rock")));
    }

    /** "genre" is required to construct the object. */
    @Test public void fromMap_NoGenre() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null name");
        assertNull(Genre.fromMap(filterKey(genreMap, "genre")));
    }
}
