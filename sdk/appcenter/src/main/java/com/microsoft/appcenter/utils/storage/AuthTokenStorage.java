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
     * Retrieves home account id value.
     *
     * @return unique identifier of user.
     */
    String getHomeAccountId();

    /**
     * Gets the oldest token info from history. It contains a token and time
     * when it was valid. To retrieve the next history entry, the oldest one
     * should be removed (once it isn't required anymore).
     *
     * @return auth token info.
     * @see AuthTokenInfo
     */
    AuthTokenInfo getOldestToken();

    /**
     * Removes the token from history.
     * TODO remove only oldest, use set null for sign out
     *
     * @param token auth token to remove.
     */
    void removeToken(String token);
}
