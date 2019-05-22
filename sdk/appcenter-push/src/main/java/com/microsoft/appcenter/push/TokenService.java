/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.push.Push.LOG_TAG;

/**
 * A service to handle the creation, rotation, and updating of registration tokens.
 */
public class TokenService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        AppCenterLog.debug(LOG_TAG, "Received push token update via service callback.");
        Push.getInstance().onTokenRefresh(token);
    }
}
