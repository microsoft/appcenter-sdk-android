/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

public interface AuthProvider {

    /**
     * Implement this method and pass a fresh authentication token using the callback.
     * This will be called 1 time right after registering and also whenever the token is about to expire.
     *
     * @param callback callback to provide the result.
     */
    void acquireToken(Callback callback);

    interface Callback {

        /**
         * Notify SDK that authentication completed.
         *
         * @param jwt token value or null if authentication failed.
         */
        void onAuthResult(String jwt);
    }
}
