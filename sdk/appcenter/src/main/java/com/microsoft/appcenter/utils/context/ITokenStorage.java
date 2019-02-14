package com.microsoft.appcenter.utils.context;

/**
 * Interface for storage that works with token.
 */
public interface ITokenStorage {

    /**
     * Stores token value.
     *
     * @param token auth token.
     */
    void saveToken(String token);

    /**
     * Retrieves token value.
     *
     * @return auth token.
     */
    String getToken();

    /**
     * Removes token value.
     */
    void removeToken();
}
