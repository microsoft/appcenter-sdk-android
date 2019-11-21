/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Device;

/**
 * Model class that correlates Device to a crash at app relaunch.
 */
public class DeviceHistory implements Comparable<Long> {

    public static String KEY_TIMESTAMP = "mTimestamp";
    public static String KEY_DEVICE = "mDevice";

    private long mTimestamp;
    private Device mDevice;

    public DeviceHistory(long getTimestamp, Device getDevice) {
        mTimestamp = getTimestamp;
        mDevice = getDevice;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public Device getGetDevice() {
        return mDevice;
    }

    @Override
    public int compareTo(@NonNull Long timestamp) {
        return mTimestamp > timestamp ? 1 : 0;
    }
}
