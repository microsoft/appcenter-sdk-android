package com.microsoft.appcenter.storage.models;

import java.util.Calendar;

public abstract class BaseOptions {

    public static final int INFINITE = -1;
    public static final int NO_CACHE = 0;
    public static final int DEFAULT_ONE_HOUR = 60 * 60;

    protected BaseOptions() {
        this.mTtl = DEFAULT_ONE_HOUR;
    }

    protected BaseOptions(int ttl) {
        if (ttl < -1) throw new IllegalArgumentException("Time-to-live should be greater than or equal to zero, or -1 for infinite.");
        this.mTtl = ttl;
    }

    private int mTtl;

    /**
     * @return document time-to-live in seconds (default to 1 hour)
     */
    public int getDeviceTimeToLive() { return mTtl; }


    /**
     * Set document time-to-live in seconds
     */
    public void setDeviceTimeToLive(int ttl) {
        this.mTtl = ttl;
    }

    public boolean isExpired(long lastModified) {
        long documentAge = Calendar.getInstance().getTimeInMillis() - lastModified;
        return documentAge > mTtl *1000;
    }
}
