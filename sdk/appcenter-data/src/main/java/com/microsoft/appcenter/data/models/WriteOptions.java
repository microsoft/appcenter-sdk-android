/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

public class WriteOptions extends BaseOptions {

    public WriteOptions() {
        super();
    }

    public WriteOptions(int ttl) {
        super(ttl);
    }

    public static WriteOptions createInfiniteCacheOptions() {
        return new WriteOptions(BaseOptions.INFINITE);
    }

    public static WriteOptions createNoCacheOptions() {
        return new WriteOptions(BaseOptions.NO_CACHE);
    }
}
