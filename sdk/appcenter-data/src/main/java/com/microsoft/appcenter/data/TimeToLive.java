/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.appcenter.data;

/**
 * Constants defining time-to-live in seconds.
 */
public final class TimeToLive {

    /**
     * Cache does not expire.
     */
    public static final int INFINITE = -1;

    /**
     * Do not cache documents.
     */
    public static final int NO_CACHE = 0;

    /**
     * Default caching value is cache does not expire.
     */
    public static final int DEFAULT = INFINITE;
}
