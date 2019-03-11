// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.appcenter.storage.models;

public class WriteOptions extends BaseOptions {

    public WriteOptions() {
        super();
    }

    public WriteOptions(int ttl) {
        super(ttl);
    }

    public static WriteOptions createInfiniteCacheOption() {
        return new WriteOptions(BaseOptions.INFINITE);
    }

    public static WriteOptions createNoCacheOption() {
        return new WriteOptions(BaseOptions.NO_CACHE);
    }
}
