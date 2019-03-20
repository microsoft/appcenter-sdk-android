/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import com.microsoft.appcenter.utils.context.AuthTokenInfo;

import java.util.Date;

/**
 * Interface for storage that works with token.
 */
public interface AuthTokenStorage {

    /**
     * Stores token value along with the corresponding account id.
     *
     * @param token         auth token.
     * @param homeAccountId unique identifier of user.
     * @param expiresOn     time when token expires.
     */
    void saveToken(String token, String homeAccountId, Date expiresOn);

    /**
     * Retrieves token value.
     *
     * @return auth token.
     */
    String getToken();

    /**
     * Retrieves homeAccountId value.
     *
     * @return unique identifier of user.
     */
    String getHomeAccountId();

    /**
     * Gets the oldest token info from history.
     *
     * @return auth token info.
     */
    AuthTokenInfo getOldestToken();

    /**
     * Removes the token from history.
     *
     * @param token auth token to remove.
     */
    void removeToken(String token);
}
