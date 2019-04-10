/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.Constants.COMMON_SCHEMA_PREFIX_SEPARATOR;

/**
 * Utility to store and retrieve values for user identifiers.
 */
public class UserIdContext {

    /**
     * Custom App User ID prefix for Common Schema.
     */
    private static final String CUSTOM_PREFIX = "c";

    /**
     * Maximum allowed length for user identifier for App Center server.
     */
    @VisibleForTesting
    public static final int USER_ID_APP_CENTER_MAX_LENGTH = 256;

    /**
     * Unique instance.
     */
    private static UserIdContext sInstance;

    /**
     * Current user identifier.
     */
    private String mUserId;

    /**
     * Global listeners collection.
     */
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap<Listener, Boolean>());

    /**
     * Get unique instance.
     *
     * @return unique instance.
     */
    public static synchronized UserIdContext getInstance() {
        if (sInstance == null) {
            sInstance = new UserIdContext();
        }
        return sInstance;
    }

    @VisibleForTesting
    public static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check if userId is valid for One Collector.
     *
     * @param userId userId.
     * @return true if valid, false otherwise.
     */
    public static boolean checkUserIdValidForOneCollector(String userId) {
        if (userId == null) {
            return true;
        }
        if (userId.isEmpty()) {
            AppCenterLog.error(LOG_TAG, "userId must not be empty.");
            return false;
        }
        int prefixIndex = userId.indexOf(COMMON_SCHEMA_PREFIX_SEPARATOR);
        if (prefixIndex >= 0) {
            String prefix = userId.substring(0, prefixIndex);
            if (!prefix.equals(CUSTOM_PREFIX)) {
                AppCenterLog.error(LOG_TAG, String.format("userId prefix must be '%s%s', '%s%s' is not supported.", CUSTOM_PREFIX, COMMON_SCHEMA_PREFIX_SEPARATOR, prefix, COMMON_SCHEMA_PREFIX_SEPARATOR));
                return false;
            } else if (prefixIndex == userId.length() - 1) {
                AppCenterLog.error(LOG_TAG, "userId must not be empty.");
                return false;
            }
        }
        return true;
    }

    /**
     * Check if userId is valid for App Center.
     *
     * @param userId userId.
     * @return true if valid, false otherwise.
     */
    public static boolean checkUserIdValidForAppCenter(String userId) {
        if (userId != null && userId.length() > USER_ID_APP_CENTER_MAX_LENGTH) {
            AppCenterLog.error(LOG_TAG, "userId is limited to " + USER_ID_APP_CENTER_MAX_LENGTH + " characters.");
            return false;
        }
        return true;
    }

    /**
     * Add 'c:' prefix to userId if the userId has no prefix.
     *
     * @param userId userId.
     * @return prefixed userId or null if the userId was null.
     */
    public static String getPrefixedUserId(String userId) {
        if (userId != null && !userId.contains(COMMON_SCHEMA_PREFIX_SEPARATOR)) {
            return CUSTOM_PREFIX + COMMON_SCHEMA_PREFIX_SEPARATOR + userId;
        }
        return userId;
    }

    /**
     * Adds listener to user context.
     *
     * @param listener listener to be notified of changes.
     */
    public void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a specific listener.
     *
     * @param listener listener to be removed.
     */
    public void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Get current identifier.
     *
     * @return user identifier.
     */
    public synchronized String getUserId() {
        return mUserId;
    }

    /**
     * Set current user identifier.
     *
     * @param userId user identifier.
     */
    public void setUserId(String userId) {
        if (!updateUserId(userId)) {
            return;
        }

        /* Call listeners so that they can react on new token. */
        for (Listener listener : mListeners) {
            listener.onNewUserId(mUserId);
        }
    }

    /**
     * Update user identifier.
     *
     * @param userId user identifier.
     * @return true if user identifier is updated.
     */
    private synchronized boolean updateUserId(String userId) {
        if (TextUtils.equals(mUserId, userId)) {
            return false;
        }
        mUserId = userId;
        return true;
    }

    public interface Listener {

        /**
         * Called whenever a new user id set.
         *
         * @param userId user identifier.
         */
        void onNewUserId(String userId);
    }
}
