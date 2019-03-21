/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

public class ReadOptions extends BaseOptions {

    public ReadOptions() {
        super();
    }

    public ReadOptions(int ttl) {
        super(ttl);
    }

    public static ReadOptions CreateInfiniteCacheOption() {
        return new ReadOptions(BaseOptions.INFINITE);
    }

    public static ReadOptions CreateNoCacheOption() {
        return new ReadOptions(BaseOptions.NO_CACHE);
    }
}
