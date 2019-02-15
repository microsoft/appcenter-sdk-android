package com.microsoft.appcenter.utils.context;

import android.content.Context;

/**
 * Factory class to produce instance of {@link AuthTokenStorage}.
 */
class TokenStorageFactory {

    /**
     * Retrieves current implementation of {@link AuthTokenStorage}.
     *
     * @param context application context.
     * @return instance of {@link AuthTokenStorage}.
     */
    static AuthTokenStorage getTokenStorage(Context context) {
        return new PreferenceTokenStorage(context);
    }
}
