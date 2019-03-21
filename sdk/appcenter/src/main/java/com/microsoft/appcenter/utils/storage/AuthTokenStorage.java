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
     * Removes the token from history. Please note that only oldest token is
     * allowed to remove. To reset current to anonymous, use
     * {@link #saveToken(String, String, Date)} with <code>null</code> value instead.
     *
     * @param token auth token to remove. Despite the fact that only the oldest
     *              token can be removed, it's required to avoid removing
     *              the wrong one on duplicated calls etc.
     */
    void removeToken(String token);
}
