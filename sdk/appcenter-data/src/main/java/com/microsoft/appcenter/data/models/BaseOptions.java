/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.data.TimeToLive;

public abstract class BaseOptions {

    private int mTtl;

    BaseOptions() {
        this(TimeToLive.DEFAULT);
    }

    BaseOptions(int ttl) {
        if (ttl < -1) {
            throw new IllegalArgumentException("Time-to-live should be greater than or equal to zero, or -1 for infinite.");
        }
        this.mTtl = ttl;
    }

    /**
     * @return document time-to-live in seconds.
     */
    public int getDeviceTimeToLive() {
        return mTtl;
    }
}
