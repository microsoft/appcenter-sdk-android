/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.storage.AuthTokenStorage;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Utility to store and retrieve the latest authorization token.
 */
public class AuthTokenContext {

    /**
     * Unique instance.
     */
    private static AuthTokenContext sInstance;

    /**
     * Global listeners collection.
     */
    private final Collection<Listener> mListeners = new LinkedHashSet<>();

    /**
     * Current value of auth token.
     */
    private String mAuthToken;

    /**
     * Current value of home account id.
     */
    private String mHomeAccountId;

    /**
     * Instance of {@link AuthTokenStorage} to store token information.
     */
    private AuthTokenStorage mStorage;

    /**
     * Get unique instance.
     *
     * @return unique instance.
     */
    public static synchronized AuthTokenContext getInstance() {
        if (sInstance == null) {
            sInstance = new AuthTokenContext();
        }
        return sInstance;
    }

    /**
     * Unset singleton instance.
     */
    @VisibleForTesting
    public static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Adds listener to token context.
     *
     * @param listener listener to be notified of changes.
     */
    public synchronized void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a specific listener.
     *
     * @param listener listener to be removed.
     */
    public synchronized void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Gets current authorization token.
     *
     * @return authorization token.
     */
    public synchronized String getAuthToken() {
        return mAuthToken;
    }

    /**
     * Gets current homeAccountId value.
     *
     * @return unique identifier of user.
     */
    public synchronized String getHomeAccountId() {
        return mHomeAccountId;
    }

    /**
     * Sets new authorization token.
     *
     * @param authToken     authorization token.
     * @param homeAccountId unique user id.
     */
    public synchronized void setAuthToken(String authToken, String homeAccountId) {
        if (mStorage != null) {
            mStorage.saveToken(authToken, homeAccountId);
        }
        updateAuthToken(authToken, homeAccountId);
    }

    private void updateAuthToken(String authToken, String homeAccountId) {
        final boolean isNewUser = isNewUser(homeAccountId);
        mAuthToken = authToken;
        mHomeAccountId = homeAccountId;

        /* Call listeners so that they can react on new token. */
        for (Listener listener : mListeners) {
            listener.onNewAuthToken(authToken);
            if (isNewUser) {
                listener.onNewUser(authToken);
            }
        }
    }

    /**
     * Check whether the user is new.
     *
     * @param newHomeAccountId account id of the logged in user.
     * @return true if this user is not the same as previous, false otherwise.
     */
    private synchronized boolean isNewUser(String newHomeAccountId) {
        return mHomeAccountId == null || !mHomeAccountId.equals(newHomeAccountId);
    }

    /**
     * Clears info about the token.
     */
    public synchronized void clearAuthToken() {
        setAuthToken(null, null);
    }

    public synchronized void cacheAuthToken() {
        if (mStorage != null) {
            updateAuthToken(mStorage.getToken(), mStorage.getHomeAccountId());
        }
    }

    /**
     *
     *
     * @return
     */
    public AuthTokenStorage getStorage() {
        return mStorage;
    }

    /**
     *
     *
     * @param storage
     */
    public void setStorage(AuthTokenStorage storage) {
        mStorage = storage;
    }

    /**
     * Token context global listener specification.
     */
    public interface Listener {

        /**
         * Called whenever a new token is set.
         */
        void onNewAuthToken(String authToken);

        /**
         * Called whenever a new user signs in.
         */
        void onNewUser(String authToken);
    }
}
