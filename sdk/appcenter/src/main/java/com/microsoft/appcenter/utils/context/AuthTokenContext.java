package com.microsoft.appcenter.utils.context;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

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
     * Global listeners.
     */
    private final Collection<Listener> mListeners;

    /**
     * Storage that handles saving and encrypting token.
     */
    private ITokenStorage mTokenStorage;

    /**
     * Get unique instance.
     *
     * @return unique instance.
     */
    public static synchronized AuthTokenContext getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AuthTokenContext(context);
        }
        return sInstance;
    }

    /**
     * A private constructor for the class.
     *
     * @param context application context instance.
     */
    private AuthTokenContext(Context context) {
        mTokenStorage = TokenStorageFactory.getTokenStorage(context);
        mListeners = new LinkedHashSet<>();
    }

    /**
     * Unsets singleton instance.
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
    public synchronized void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a specific listener.
     *
     * @param listener listener to be removed.
     */
    public synchronized void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Gets current authorization token.
     *
     * @return authorization token.
     */
    public synchronized String getAuthToken() {
        return mTokenStorage.getToken();
    }

    /**
     * Sets new authorization token.
     *
     * @param authToken authorization token.
     */
    public synchronized void setAuthToken(String authToken) {
        mTokenStorage.saveToken(authToken);

        /* Call listeners so that they can react on new token. */
        for (Listener listener : mListeners) {
            listener.onNewToken(authToken);
        }
    }

    /**
     * Token context global listener specification.
     */
    interface Listener {

        /**
         * Called whenever a new token is set.
         */
        void onNewToken(String authToken);
    }
}

