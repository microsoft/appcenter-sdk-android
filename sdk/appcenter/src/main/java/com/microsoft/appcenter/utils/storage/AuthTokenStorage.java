/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import com.microsoft.appcenter.utils.context.AuthTokenInfo;

/**
 * Interface for storage that works with token.
 */
public interface AuthTokenStorage {

    /**
     * Stores token value along with the corresponding account id.
     *
     * @param token         auth token.
     * @param homeAccountId unique identifier of user.
     */
    void saveToken(String token, String homeAccountId);

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
     *
     *
     * @return
     */
    AuthTokenInfo getOldestToken();

    /**
     *
     *
     * @param token
     */
    void removeToken(String token);
}
