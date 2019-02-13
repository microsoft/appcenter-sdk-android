package com.microsoft.appcenter.utils.context;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Utility to store and retrieve the latest authorization token.
 */
public class AuthTokenContext {

    /**
     * Global listeners.
     */
    private final Collection<Listener> mListeners;

    /**
     * Storage that handles saving and encrypting token.
     */
    private TokenStorage mTokenStorage;

    /**
     * Initializes token context with the given token storage.
     * @param tokenStorage token storage to save token.
     */
    public AuthTokenContext(TokenStorage tokenStorage) {
        mTokenStorage = tokenStorage;
        mListeners = new LinkedHashSet<>();
    }

    /**
     * Adds listener to token context.
     * @param listener listener to be notified of changes.
     */
    public synchronized void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a specific listener.
     * @param listener listener to be removed.
     */
    public synchronized void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Get current authorization token.
     *
     * @return authorization token.
     */
    public synchronized String getAuthToken() {
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

