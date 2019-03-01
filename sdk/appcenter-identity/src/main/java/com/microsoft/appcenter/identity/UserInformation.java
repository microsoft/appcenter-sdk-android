package com.microsoft.appcenter.identity;

/**
 * User information.
 */
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
    UserInformation(String accountId) {
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
