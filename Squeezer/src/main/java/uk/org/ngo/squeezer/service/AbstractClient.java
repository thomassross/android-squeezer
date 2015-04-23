/*
 * Copyright (c) 2015 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.service;

import android.support.annotation.NonNull;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;

public abstract class AbstractClient {
    /** Shared event bus for status changes. */
    @NonNull
    protected static EventBus mEventBus;

    /**
     *
     * @param eventBus The eventbus to which connection lifecycle events will be posted.
     */
    public AbstractClient(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
    }

    /**
     * Start the connection state machine.
     */
    public void initialize() {
        mEventBus.postSticky(new ConnectionChanged(ConnectionState.DISCONNECTED));
    }

    /**
     * Disconnect from the server.
     *
     * @param loginFailed The disconnection is because of a login failure, and not a user-initiated
     * action.
     */
    abstract void disconnect(boolean loginFailed);



}
