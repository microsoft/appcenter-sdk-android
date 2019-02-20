package com.microsoft.appcenter.utils.context;

/**
 * Empty implementation to make callbacks optional.
 */
public class AbstractTokenContextListener implements  AuthTokenContext.Listener {

    @Override
    public void onNewAuthToken(String authToken) {
    }

    @Override
    public void onNewUser(String authToken) {
    }
}
