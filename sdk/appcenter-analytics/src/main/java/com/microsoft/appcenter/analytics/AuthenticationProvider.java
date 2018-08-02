package com.microsoft.appcenter.analytics;

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
     * Token provider object that will be called to provide a current authentication token.
     *
     * @return the token provider.
     */
    TokenProvider getTokenProvider() {
        return mTokenProvider;
    }

    /**
     * The type of the authentication provider, e.g. MSA.
     */
    public enum Type {
        MSA
    }

    /**
     * Implement this interface to enable updating the authentication token.
     */
    private interface TokenProvider {

        /**
         * Implement this method and return a current authentication token.
         *
         * @param ticketKey The ticket key that is used to get an updated token.
         * @return the updated tocken.
         */
        String getToken(String ticketKey);
    }


}
