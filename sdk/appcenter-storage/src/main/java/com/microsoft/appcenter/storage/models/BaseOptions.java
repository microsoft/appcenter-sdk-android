/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

import java.util.Calendar;

public abstract class BaseOptions {

    /**
     * Cache does not expire
     */
    public static final int INFINITE = -1;

    /**
     * Do not cache documents locally
     */
    public static final int NO_CACHE = 0;

    /**
     * Default caching value of one hour
     */
    public static final int DEFAULT_ONE_HOUR = 60 * 60;

    private int mTtl;

    BaseOptions() {
        this(DEFAULT_ONE_HOUR);
    }

    BaseOptions(int ttl) {
        if (ttl < -1) {
            throw new IllegalArgumentException("Time-to-live should be greater than or equal to zero, or -1 for infinite.");
        }
        this.mTtl = ttl;
    }

    /**
     * @return document time-to-live in seconds (default to 1 hour)
     */
    public int getDeviceTimeToLive() {
        return mTtl;
    }

    /**
     * @param lastModified timestamp of when the document was last modified
     * @return whether a document with the specified TTL has expired since it was last modified
     */
    public boolean isExpired(long lastModified) {
        long documentAge = Calendar.getInstance().getTimeInMillis() - lastModified;
        return documentAge > mTtl * 1000;
    }
}
