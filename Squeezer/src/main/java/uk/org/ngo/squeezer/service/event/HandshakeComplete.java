/*
 * Copyright (c) 2014 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.service.event;

import com.google.auto.value.AutoValue;

/**
 * Event sent after handshaking with the server is complete.
 */
@AutoValue
public abstract class HandshakeComplete {
    /** Does the server support the {@code favorites items} command? */
    public abstract boolean canFavourites();

    /** Does the server support the {@code musicfolders} command? */
    public abstract boolean canMusicFolders();

    /** Does the server support the {@code myapps items} command? */
    public abstract boolean canMyApps();

    /** Does the server support the {@code randomplay} command? */
    public abstract boolean canRandomPlay();

    /** Server version */
    public abstract String version();

    public static HandshakeComplete create(boolean canFavourites, boolean canMusicFolders,
                             boolean canMyApps, boolean canRandomPlay, String version) {
        return new AutoValue_HandshakeComplete(
                canFavourites, canMusicFolders, canMyApps, canRandomPlay, version
        );
    }
}
