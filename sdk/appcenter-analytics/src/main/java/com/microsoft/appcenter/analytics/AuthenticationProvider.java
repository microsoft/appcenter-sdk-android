package com.microsoft.appcenter.analytics;

import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.HashUtils;
import com.microsoft.appcenter.utils.TicketCache;

import static com.microsoft.appcenter.analytics.Analytics.LOG_TAG;

public class AuthenticationProvider {

    /**
     * The type.
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
     * Create a new authentication provider.
     *
     * @param type          The type for the provider, e.g. MSA.
     * @param ticketKey     The ticket key for the provider.
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
        AsyncTaskUtils.execute(LOG_TAG, new TokenCall(this));
    }

    /**
     * The type of the authentication provider, e.g. MSA.
     */
    public enum Type {
        MSA

    }

    /**
     * Application callback to request authentication token value.
     */
    public interface TokenProvider {

        /**
         * Implement this method and return a current authentication token.
         *
         * @param ticketKey The ticket key that is used to get an updated token.
         * @return the updated token.
         */
        String getToken(String ticketKey);

    }

    @VisibleForTesting
    static class TokenCall extends AsyncTask<Void, Void, String> {

        /**
         * Authentication provider for this call.
         */
        private final AuthenticationProvider mAuthenticationProvider;

        /**
         * Init.
         */
        TokenCall(AuthenticationProvider authenticationProvider) {
            mAuthenticationProvider = authenticationProvider;
        }

        @Override
        protected String doInBackground(Void... params) {
            String ticketKey = mAuthenticationProvider.mTicketKey;
            try {
                return mAuthenticationProvider.getTokenProvider().getToken(ticketKey);
            } catch (RuntimeException e) {
                AppCenterLog.error(LOG_TAG, "Failed to get token for key=" + ticketKey, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String token) {
            TicketCache.getInstance().putTicket(mAuthenticationProvider.mTicketKeyHash, token);
        }
    }
}
