/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

/**
 * Empty implementation to make callbacks optional.
 */
public abstract class AbstractTokenContextListener implements AuthTokenContext.Listener {

    @Override
    public void onNewAuthToken(String authToken) {
    }

    @Override
    public void onNewUser(String accountId) {
    }

    @Override
    public void onTokenRequiresRefresh(String homeAccountId) {
    }
}
