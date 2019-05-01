/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AppCenter;

import java.util.Random;
import java.util.UUID;

/**
 * UUID utils.
 */
public class UUIDUtils {

    @VisibleForTesting
    static Implementation sImplementation = new Implementation() {

        @Override
        public UUID randomUUID() {
            return UUID.randomUUID();
        }
    };

    /**
     * Random used when SecureRandom fails to initialize on some devices...
     */
    private static Random sRandom;

    /**
     * Utils pattern hides constructor.
     */
    @VisibleForTesting
    UUIDUtils() {
    }

    /**
     * Get a version 4 variant 2 random UUID.
     *
     * @return random UUID.
     */
    public static UUID randomUUID() {
        try {
            return sImplementation.randomUUID();
        } catch (SecurityException e) {

            /* Some devices can crash while allocating a SecureRandom, used by UUID, fall back... */
            initFailOver(e);
            long highest = (sRandom.nextLong() & -61441L) | 16384L;
            long lowest = (sRandom.nextLong() & 4611686018427387903L) | -9223372036854775808L;
            return new UUID(highest, lowest);
        }
    }

    /**
     * Init the fail over random the first time we hit the error.
     */
    private static synchronized void initFailOver(SecurityException e) {
        if (sRandom == null) {
            sRandom = new Random();
            AppCenterLog.error(AppCenter.LOG_TAG, "UUID.randomUUID failed, using Random as fallback", e);
        }
    }

    @VisibleForTesting
    interface Implementation {
        UUID randomUUID();
    }
}
