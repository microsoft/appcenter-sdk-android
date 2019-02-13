package com.microsoft.appcenter.utils.context;

import android.support.annotation.VisibleForTesting;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Utility to store and retrieve the latest identity token.
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
    private TokenStorage mTokenStorage;

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

    private AuthTokenContext() {
        mListeners = new LinkedHashSet<>();
    }

    public void setTokenStorage(TokenStorage tokenStorage) {
        mTokenStorage = tokenStorage;
    }

    @VisibleForTesting
    public static synchronized void unsetInstance() {
        sInstance = null;
    }

    public synchronized void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public synchronized void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Get current authorization token.
     *
     * @return authorization token.
     */
    public synchronized String getIdentityToken() {
        return mTokenStorage.getToken();
    }

    /**
     * Set new authorization token.
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

