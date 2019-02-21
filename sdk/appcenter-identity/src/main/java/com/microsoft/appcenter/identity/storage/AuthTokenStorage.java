package com.microsoft.appcenter.identity.storage;

/**
 * Interface for storage that works with token.
 */
public interface AuthTokenStorage {

    /**
     * Stores token value along with the corresponding account id.
     *
     * @param token auth token.
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
     * Removes token value.
     */
    void removeToken();

    /**
     * Gets token and the last account id from storage and caches it.
     */
    void cacheToken();
}
