/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.ingestion;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.appcenter.DependencyConfiguration;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.AbstractAppCenterIngestion;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.distribute.DistributeConstants.HEADER_API_TOKEN;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;

public class DistributeIngestion extends AbstractAppCenterIngestion {

    public DistributeIngestion(Context context) {
        super();
        HttpClient httpClient = DependencyConfiguration.getHttpClient();
        if (httpClient == null) {
            httpClient = createHttpClient(context);
        }
        setHttpClient(httpClient);
    }

    public ServiceCall checkReleaseAsync(final String appSecret, String url, Map<String, String> headers, ServiceCallback serviceCallback) {
       return getServiceCall(url, METHOD_GET, headers, new HttpClient.CallTemplate() {

            @Override
            public String buildRequestBody() {

                /* Only GET is used by Distribute service. This method is never getting called. */
                return null;
            }

            @Override
            public void onBeforeCalling(URL url, Map<String, String> headers) {
                if (AppCenterLog.getLogLevel() <= VERBOSE) {

                    /* Log url. */
                    String urlString = url.toString().replaceAll(appSecret, HttpUtils.hideSecret(appSecret));
                    AppCenterLog.verbose(LOG_TAG, "Calling " + urlString + "...");

                    /* Log headers. */
                    Map<String, String> logHeaders = new HashMap<>(headers);
                    String apiToken = logHeaders.get(HEADER_API_TOKEN);
                    if (apiToken != null) {
                        logHeaders.put(HEADER_API_TOKEN, HttpUtils.hideSecret(apiToken));
                    }
                    AppCenterLog.verbose(LOG_TAG, "Headers: " + logHeaders);
                }
            }
        }, serviceCallback);
    }
}
