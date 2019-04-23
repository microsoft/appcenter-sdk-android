/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.auth;

import com.microsoft.appcenter.UserInformation;

/**
 * Result for sign-in operation.
 */
@SuppressWarnings("WeakerAccess") // TODO remove warning when JCenter published and demo updated
public class SignInResult {

    /**
     * User information.
     */
    private final UserInformation mUserInformation;

    /**
     * Error that may have occurred during sign-in.
     */
    private final Exception mException;

    /**
     * Init.
     *
     * @param userInformation User information.
     * @param exception       Exception if an error occurred during sign-in.
     */
    SignInResult(UserInformation userInformation, Exception exception) {
        mUserInformation = userInformation;
        mException = exception;
    }

    /**
     * Get the exception that caused sign-in to fail.
     *
     * @return Exception for sign-in failure or null if sign-in was successful.
     */
    public Exception getException() {
        return mException;
    }

    /**
     * Get user information.
     *
     * @return User information or null if an error occurred.
     */
    @SuppressWarnings("WeakerAccess")
    // TODO remove warning once published to jCenter and used in test app
    public UserInformation getUserInformation() {
        return mUserInformation;
    }
}
