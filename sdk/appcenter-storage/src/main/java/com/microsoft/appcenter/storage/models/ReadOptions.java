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

    public static ReadOptions createInfiniteCacheOptions() {
        return new ReadOptions(BaseOptions.INFINITE);
    }

    public static ReadOptions createNoCacheOptions() {
        return new ReadOptions(BaseOptions.NO_CACHE);
    }

    /**
     * @param expiredAt timestamp of when the document is expired.
     * @return whether a document is expired.
     */
    public static boolean isExpired(long expiredAt) {
        if (expiredAt == BaseOptions.INFINITE) {
            return false;
        }
        return System.currentTimeMillis() >= expiredAt;
    }
}
