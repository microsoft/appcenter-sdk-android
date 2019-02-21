package com.microsoft.appcenter.identity.storage;

import android.content.Context;

/**
 * Factory class to produce instance of {@link AuthTokenStorage}.
 */
public class TokenStorageFactory {

    /**
     * Instance of {@link AuthTokenStorage}.
     */
    private static AuthTokenStorage sTokenStorageInstance;

    /**
     * Retrieves current implementation of {@link AuthTokenStorage}.
     *
     * @param context application context.
     * @return instance of {@link AuthTokenStorage}.
     */
    public static AuthTokenStorage getTokenStorage(Context context) {
        if (sTokenStorageInstance == null) {
            sTokenStorageInstance = new PreferenceTokenStorage(context);
        }
        return sTokenStorageInstance;
    }
}
