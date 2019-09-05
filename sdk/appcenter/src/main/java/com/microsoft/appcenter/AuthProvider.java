/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

/**
 * Authentication provider used to refresh authentication tokens.
 * The implementation is defined by applications.
 */
public interface AuthProvider {

    /**
     * Implement this method and pass a fresh authentication token using the callback.
     * This will be called whenever the token is about to expire or is already expired.
     *
     * @param callback callback to provide the result.
     */
    void acquireToken(Callback callback);

    /**
     * Authentication callback.
     */
    interface Callback {

        /**
         * Notify SDK that authentication completed.
         *
         * @param jwt token value (in JWT format) or null if authentication failed.
         */
        void onAuthResult(String jwt);
    }
}
