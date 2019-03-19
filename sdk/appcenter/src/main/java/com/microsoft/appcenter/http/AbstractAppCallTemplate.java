/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.Constants.APP_SECRET;
import static com.microsoft.appcenter.Constants.AUTHORIZATION_HEADER;

/**
 * Common logic between calls that have app secret and authorization headers.
 */
public abstract class AbstractAppCallTemplate implements HttpClient.CallTemplate {

    @Override
    public void onBeforeCalling(URL url, Map<String, String> headers) {
        if (AppCenterLog.getLogLevel() <= VERBOSE) {

            /* Log url. */
            AppCenterLog.verbose(LOG_TAG, "Calling " + url + "...");

            /* Log headers. */
            Map<String, String> logHeaders = new HashMap<>(headers);
            String appSecret = logHeaders.get(APP_SECRET);
            if (appSecret != null) {
                logHeaders.put(APP_SECRET, HttpUtils.hideSecret(appSecret));
            }
            String authToken = logHeaders.get(AUTHORIZATION_HEADER);
            if (authToken != null) {
                logHeaders.put(AUTHORIZATION_HEADER, HttpUtils.hideAuthToken(authToken));
            }
            AppCenterLog.verbose(LOG_TAG, "Headers: " + logHeaders);
        }
    }
}
