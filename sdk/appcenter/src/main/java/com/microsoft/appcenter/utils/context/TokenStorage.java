package com.microsoft.appcenter.utils.context;

/**
 * Interface for storages that work with token.
 */
public interface TokenStorage {

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
