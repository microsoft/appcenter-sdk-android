/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

public abstract class BaseOptions {

    /**
     * Cache does not expire.
     */
    public static final int INFINITE = -1;

    /**
     * Do not cache documents locally.
     */
    public static final int NO_CACHE = 0;

    /**
     * Default caching value of one day.
     */
    public static final int DEFAULT_EXPIRATION_IN_SECONDS = 60 * 60 * 24;

    private int mTtl;

    BaseOptions() {
        this(DEFAULT_EXPIRATION_IN_SECONDS);
    }

    BaseOptions(int ttl) {
        if (ttl < -1) {
            throw new IllegalArgumentException("Time-to-live should be greater than or equal to zero, or -1 for infinite.");
        }
        this.mTtl = ttl;
    }

    /**
     * @return document time-to-live in seconds (default to one day).
     */
    public int getDeviceTimeToLive() {
        return mTtl;
    }
}
