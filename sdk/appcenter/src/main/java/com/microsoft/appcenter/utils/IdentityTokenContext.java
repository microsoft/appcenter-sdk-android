package com.microsoft.appcenter.utils;

import android.support.annotation.VisibleForTesting;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Utility to store and retrieve the latest identity token.
 */
public class IdentityTokenContext {

    /**
     * Unique instance.
     */
    private static IdentityTokenContext sInstance;

    /**
     * Current identity token.
     */
    private String mIdentityToken;


    /**
     * Global listeners.
     */
    private final Collection<Listener> mListeners;

    /**
     * Get unique instance.
     *
     * @return unique instance.
     */
    public static synchronized IdentityTokenContext getInstance() {
        if (sInstance == null) {
            sInstance = new IdentityTokenContext();
        }
        return sInstance;
    }

    private IdentityTokenContext() {
        mListeners = new LinkedHashSet<>();
        mIdentityToken = ""; // TODO [Identity56021] def. value?
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
     * Get current identity token.
     *
     * @return identity id token.
     */
    public synchronized String getIdentityToken() {
        // TODO [Identity56021] Decrypt/use MSAL Cache ?
        return mIdentityToken;
    }

    /**
     * Set new identity token.
     *
     * @param identityToken identity id token.
     */
    public synchronized void setIdentityToken(String identityToken) {

        // TODO [Identity56021] Encrypt/use MSAL Cache ?
        mIdentityToken = identityToken;

        /* Call listeners so that they can react on new token. */
        for (Listener listener : mListeners) {
            listener.onNewToken();
        }
    }

    /**
     * Token context global listener specification.
     */
    interface Listener {

        /**
         * Called whenever a new token is set.
         */
        void onNewToken();
    }
}

