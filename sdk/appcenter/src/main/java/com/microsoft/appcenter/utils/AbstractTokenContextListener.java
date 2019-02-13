package com.microsoft.appcenter.utils;

/**
 * Empty implementation to make callbacks optional.
 */
public class AbstractTokenContextListener implements IdentityTokenContext.Listener {

    @Override
    public void onNewToken() {
    }
}
