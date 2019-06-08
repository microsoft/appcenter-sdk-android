/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.auth;

/**
 * Result for sign-in operation.
 */
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
    public UserInformation getUserInformation() {
        return mUserInformation;
    }
}
