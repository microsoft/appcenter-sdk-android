/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.auth;

/**
 * User information.
 */
public class UserInformation {

    /**
     * User account identifier.
     */
    private final String mAccountId;

    /**
     * Access token for user.
     */
    private final String mAccessToken;

    /**
     * Id token for user.
     */
    private final String mIdToken;

    /**
     * Init.
     *
     * @param accountId   User account identifier.
     * @param accessToken Access token for user.
     * @param idToken     Id token for user.
     */
    UserInformation(String accountId, String accessToken, String idToken) {
        mAccountId = accountId;
        mAccessToken = accessToken;
        mIdToken = idToken;
    }

    /**
     * Get user account identifier.
     *
     * @return User account identifier.
     */
    public String getAccountId() {
        return mAccountId;
    }

    /**
     * Get access token.
     *
     * @return Access token.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Get Id token.
     *
     * @return Id token.
     */
    public String getIdToken() {
        return mIdToken;
    }
}
