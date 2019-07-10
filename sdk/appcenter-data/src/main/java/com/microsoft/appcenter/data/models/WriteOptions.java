/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.data.TimeToLive;

public class WriteOptions extends BaseOptions {

    public WriteOptions() {
        super();
    }

    public WriteOptions(int ttl) {
        super(ttl);
    }

    public static WriteOptions createInfiniteCacheOptions() {
        return new WriteOptions(TimeToLive.INFINITE);
    }

    public static WriteOptions createNoCacheOptions() {
        return new WriteOptions(TimeToLive.NO_CACHE);
    }

    public static WriteOptions ensureNotNull(WriteOptions writeOptions) {
        return writeOptions == null ? new WriteOptions() : writeOptions;
    }
}
