/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import com.microsoft.appcenter.utils.context.AuthTokenInfo;

import java.util.Date;
import java.util.List;

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
     * Gets the token history. It contains tokens and time when it was valid.
     *
     * @return auth token info.
     * @see AuthTokenInfo
     */
    List<AuthTokenInfo> getTokenHistory();

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
