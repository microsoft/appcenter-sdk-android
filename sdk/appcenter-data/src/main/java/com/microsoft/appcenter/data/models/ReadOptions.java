/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.data.TimeToLive;

public class ReadOptions extends BaseOptions {

    public ReadOptions() {
        super();
    }

    public ReadOptions(int ttl) {
        super(ttl);
    }

    public static ReadOptions createInfiniteCacheOptions() {
        return new ReadOptions(TimeToLive.INFINITE);
    }

    public static ReadOptions createNoCacheOptions() {
        return new ReadOptions(TimeToLive.NO_CACHE);
    }

    /**
     * @param expiredAt timestamp of when the document is expired.
     * @return whether a document is expired.
     */
    public static boolean isExpired(long expiredAt) {
        if (expiredAt == TimeToLive.INFINITE) {
            return false;
        }
        return System.currentTimeMillis() >= expiredAt;
    }

    public static ReadOptions ensureNotNull(ReadOptions readOptions) {
        return readOptions == null ? new ReadOptions() : readOptions;
    }
}
