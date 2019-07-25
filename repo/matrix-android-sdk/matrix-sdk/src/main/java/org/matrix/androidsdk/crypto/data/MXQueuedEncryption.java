/*
 * Copyright 2016 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.androidsdk.crypto.data;

import com.google.gson.JsonElement;

import org.matrix.androidsdk.rest.callback.ApiCallback;

public class MXQueuedEncryption {

    /**
     * The data to encrypt.
     */
    public JsonElement mEventContent;
    public String mEventType;

    /**
     * the asynchronous callback
     */
    public ApiCallback<JsonElement> mApiCallback;
}