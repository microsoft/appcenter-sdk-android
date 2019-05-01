/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HashUtils;
import com.microsoft.appcenter.utils.TicketCache;

import java.util.Date;

import static com.microsoft.appcenter.Constants.COMMON_SCHEMA_PREFIX_SEPARATOR;
import static com.microsoft.appcenter.analytics.Analytics.LOG_TAG;

/**
 * Authentication provider to associate logs with user identifier.
 */
public class AuthenticationProvider {

    /**
     * Number of milliseconds to refresh token before it expires.
     */
    private static final long REFRESH_THRESHOLD = 10 * 60 * 1000;

    /**
     * The authentication provider type.
     */
    private final Type mType;

    /**
     * The ticket key for this authentication provider.
     */
    private final String mTicketKey;

    /**
     * The ticket key as hash.
     */
    private final String mTicketKeyHash;

    /**
     * The token provider that will be used to get an updated authentication token.
     */
    private final TokenProvider mTokenProvider;

    /**
     * Current auth callback if we are refreshing token.
     * Used to avoid doing it more than once at a time.
     */
    private AuthenticationCallback mCallback;

    /**
     * Current token expiry date.
     */
    private Date mExpiryDate;

    /**
     * Create a new authentication provider.
     *
     * @param type          The type for the provider.
     * @param ticketKey     The ticket key for the provider. This can be any value and does not need to match user identifier.
     *                      This value will be used in the token provider callback for giving context. The only requirement
     *                      is that ticket key is different if registering a new provider (for example to switch user).
     *                      Typical implementations will pass user identifier as a value but it's not a requirement.
     * @param tokenProvider The token provider that will be used to get a current authentication token.
     */
    public AuthenticationProvider(Type type, String ticketKey, TokenProvider tokenProvider) {
        mType = type;
        mTicketKey = ticketKey;
        mTicketKeyHash = ticketKey == null ? null : HashUtils.sha256(ticketKey);
        mTokenProvider = tokenProvider;
    }

    /**
     * Get the type of this authentication provider.
     *
     * @return the type.
     */
    Type getType() {
        return mType;
    }

    /**
     * Get the ticket key for this authentication provider.
     *
     * @return the ticket key.
     */
    String getTicketKey() {
        return mTicketKey;
    }

    /**
     * Get the ticket key hash for this authentication provider.
     *
     * @return the ticket key hash.
     */
    String getTicketKeyHash() {
        return mTicketKeyHash;
    }

    /**
     * Token provider object that will be called to provide a current authentication token.
     *
     * @return the token provider.
     */
    TokenProvider getTokenProvider() {
        return mTokenProvider;
    }

    /**
     * Call token provider callback in background.
     */
    synchronized void acquireTokenAsync() {

        /* Do nothing if already acquiring token. */
        if (mCallback != null) {
            return;
        }

        /* Acquire token using a callback for result to avoid blocking this thread. */
        AppCenterLog.debug(LOG_TAG, "Calling token provider=" + mType + " callback.");
        mCallback = new AuthenticationCallback() {

            @Override
            public void onAuthenticationResult(String token, Date expiryDate) {
                handleTokenUpdate(token, expiryDate, this);
            }
        };
        mTokenProvider.acquireToken(mTicketKey, mCallback);
    }

    /**
     * Handle token callback update.
     *
     * @param token      token value.
     * @param expiryDate token expiry date.
     * @param callback   authentication callback.
     */
    private synchronized void handleTokenUpdate(String token, Date expiryDate, AuthenticationCallback callback) {

        /* Prevent multiple calls. */
        if (mCallback != callback) {
            AppCenterLog.debug(LOG_TAG, "Ignore duplicate authentication callback calls, provider=" + mType);
            return;
        }

        /* Clear callback state. */
        mCallback = null;

        /* Check parameters. */
        AppCenterLog.debug(LOG_TAG, "Got result back from token provider=" + mType);
        if (token == null) {
            AppCenterLog.error(LOG_TAG, "Authentication failed for ticketKey=" + mTicketKey);
            return;
        }
        if (expiryDate == null) {
            AppCenterLog.error(LOG_TAG, "No expiry date provided for ticketKey=" + mTicketKey);
            return;
        }

        /* Update shared cache. */
        TicketCache.putTicket(mTicketKeyHash, mType.mTokenPrefix + token);

        /* Keep track of safe expiry time. */
        mExpiryDate = expiryDate;
    }

    /**
     * Trigger asynchronous token refresh if the token is about to expire.
     */
    synchronized void checkTokenExpiry() {
        if (mExpiryDate != null && mExpiryDate.getTime() <= System.currentTimeMillis() + REFRESH_THRESHOLD) {
            acquireTokenAsync();
        }
    }

    /**
     * The supported types of the authentication provider.
     */
    public enum Type {

        /**
         * Microsoft account authentication for first party applications using compact tickets.
         */
        MSA_COMPACT("p"),

        /**
         * Microsoft account authentication for third party applications using delegate tickets.
         */
        MSA_DELEGATE("d");

        /**
         * Token value prefix.
         */
        private final String mTokenPrefix;

        /**
         * Init.
         *
         * @param tokenPrefix token value prefix.
         */
        Type(String tokenPrefix) {
            mTokenPrefix = tokenPrefix + COMMON_SCHEMA_PREFIX_SEPARATOR;
        }
    }

    /**
     * Application callback to request authentication token value.
     */
    public interface TokenProvider {

        /**
         * Implement this method and pass a fresh authentication token using the callback.
         * This will be called 1 time right after registering and also whenever the token is about to expire.
         *
         * @param ticketKey The ticket key that is used to get an updated token.
         * @param callback  callback to provide the result.
         */
        void acquireToken(@SuppressWarnings("unused") String ticketKey, AuthenticationCallback callback);
    }

    /**
     * Authentication callback.
     */
    public interface AuthenticationCallback {

        /**
         * Notify SDK that authentication completed.
         *
         * @param tokenValue token value or null if authentication failed.
         * @param expiryDate expiry date for token or null if authentication failed.
         */
        void onAuthenticationResult(String tokenValue, Date expiryDate);
    }
}
