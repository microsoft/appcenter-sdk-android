/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

/**
 * Authentication token listener used to refresh authentication tokens.
 * The implementation is defined by applications.
 */
public interface AuthTokenListener {

    /**
     * Implement this method and pass a fresh authentication token using the callback.
     * This will be called whenever the token is about to expire or is already expired.
     *
     * @param callback callback to provide the result.
     */
    void acquireAuthToken(AuthTokenCallback callback);
}
