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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Utility functions for testing model classes.
 */
class ModelTestUtil {
    /**
     * @return A copy of record with the given key removed.
     */
    @NonNull
    static Map<String, String> filterKey(@NonNull Map<String, String> record,
                                         @NonNull final String key) {
        return Maps.filterKeys(record, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable final String input) {
                if (key.equals(input)) {
                    return false;
                }
                return true;
            }
        });
    }
}
