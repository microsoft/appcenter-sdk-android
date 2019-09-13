/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.storage.CredentialsManager;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;

/**
 * TODO During release, delete this jCenter class version and move projectDependency to main folder.
 */
class BYOIUtils {

    private static Auth0 sAuth0;

    private static CredentialsManager sAuth0CredentialsManager;

    static Auth0 getAuth0Client(Context context) {
        if (sAuth0 == null) {
            sAuth0 = new Auth0(context);
            sAuth0.setOIDCConformant(true);
        }
        return sAuth0;
    }

    static CredentialsManager getAuth0CredentialsManager(Context context) {
        if (sAuth0CredentialsManager == null) {
            AuthenticationAPIClient apiClient = new AuthenticationAPIClient(getAuth0Client(context));
            sAuth0CredentialsManager = new CredentialsManager(apiClient, new SharedPreferencesStorage(context));
        }
        return sAuth0CredentialsManager;
    }

    static void setAuthTokenListener(Context context) {
    }

    static void setAuthToken(@SuppressWarnings("unused") String authToken) {
    }
}
