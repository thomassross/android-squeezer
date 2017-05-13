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

import uk.org.ngo.squeezer.service.ConnectionState;

/**
 * Event posted whenever the connection state to the server changes.
 */
@AutoValue
public abstract class ConnectionChanged {
    /** The new connection state. */
    @ConnectionState.ConnectionStates
    public abstract int connectionState();

    public static ConnectionChanged create(@ConnectionState.ConnectionStates int connectionState) {
        return new AutoValue_ConnectionChanged(connectionState);
    }
}
