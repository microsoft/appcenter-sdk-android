package com.microsoft.appcenter.utils;

import android.annotation.SuppressLint;
import android.support.annotation.VisibleForTesting;

import java.util.UUID;

/**
 * Contains the current Session ID.
 */
public class SessionIdKeeper {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static SessionIdKeeper sInstance;

    /**
     * The Session ID.
     */
    private UUID mSessionId = null;

    /**
     * Gets the instance.
     *
     * @return the shared instance.
     */
    public static synchronized SessionIdKeeper getInstance() {
        if (sInstance == null) {
            sInstance = new SessionIdKeeper();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Get the Session ID.
     *
     * @return the current Session ID, or null if one hasn't been set.
     */
    public synchronized UUID getSessionId() {
        return mSessionId;
    }

    /**
     * Resets the Session ID to a new UUID.
     *
     * @return the new Session ID.
     */
    public synchronized UUID refreshSessionId() {
        mSessionId = UUIDUtils.randomUUID();
        return mSessionId;
    }
}
