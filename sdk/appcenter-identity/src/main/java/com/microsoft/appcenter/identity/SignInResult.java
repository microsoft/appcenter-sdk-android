package com.microsoft.appcenter.identity;

/**
 * Result for sign-in operation.
 */
public class SignInResult {

    /**
     * User information.
     */
    private final UserInformation mUserInformation;

    /**
     * Error that may have occurred during sign-in.
     */
    private final Exception mException;

    /**
     * Init.
     *
     * @param userInformation User information.
     * @param exception       Exception if an error occurred during sign-in.
     */
    SignInResult(UserInformation userInformation, Exception exception) {
        mUserInformation = userInformation;
        mException = exception;
    }

    /**
     * Get error if any occurred.
     *
     * @return Error that occurred during sign-in or null if user information is available.
     */
    public Exception getException() {
        return mException;
    }

    /**
     * Get user information.
     *
     * @return User information or null if an error occurred.
     */
    public UserInformation getUserInformation() {
        return mUserInformation;
    }
}
