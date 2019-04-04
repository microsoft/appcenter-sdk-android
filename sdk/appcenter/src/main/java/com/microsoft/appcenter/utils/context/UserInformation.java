/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

/**
 * User information.
 */
@SuppressWarnings("WeakerAccess") // TODO remove warning when JCenter published and demo updated
public class UserInformation {

    /**
     * User account identifier.
     */
    private final String mAccountId;

    /**
     * Init.
     *
     * @param accountId User account identifier.
     */
    public UserInformation(String accountId) {
        mAccountId = accountId;
    }

    /**
     * Get user account identifier.
     *
     * @return User account identifier.
     */
    public String getAccountId() {
        return mAccountId;
    }
}
