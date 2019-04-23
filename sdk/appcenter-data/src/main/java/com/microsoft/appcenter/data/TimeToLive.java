package com.microsoft.appcenter.data;

/**
 * Constant around time-to-live.
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
     * Default caching value of one day.
     */
    public static final int DEFAULT = 60 * 60 * 24;
}
