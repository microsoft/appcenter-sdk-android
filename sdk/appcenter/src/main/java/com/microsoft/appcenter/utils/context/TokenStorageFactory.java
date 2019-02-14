package com.microsoft.appcenter.utils.context;

import android.content.Context;

/**
 * Factory class to produce instance of {@link ITokenStorage}.
 */
class TokenStorageFactory {

    /**
     * Retrieves current implementation of {@link ITokenStorage}.
     *
     * @param context application context.
     * @return instance of {@link ITokenStorage}.
     */
    static ITokenStorage getTokenStorage(Context context) {
        return new PreferenceTokenStorage(context);
    }
}
