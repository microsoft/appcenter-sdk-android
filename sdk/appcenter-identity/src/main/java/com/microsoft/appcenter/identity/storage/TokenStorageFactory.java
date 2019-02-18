package com.microsoft.appcenter.identity.storage;

import android.content.Context;

/**
 * Factory class to produce instance of {@link AuthTokenStorage}.
 */
public class TokenStorageFactory {

    /**
     * Retrieves current implementation of {@link AuthTokenStorage}.
     *
     * @param context application context.
     * @return instance of {@link AuthTokenStorage}.
     */
    public static AuthTokenStorage getTokenStorage(Context context) {
        return new PreferenceTokenStorage(context);
    }
}
