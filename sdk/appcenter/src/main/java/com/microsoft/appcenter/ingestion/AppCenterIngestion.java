/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.http.AbstractAppCallTemplate;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.Constants.APP_SECRET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;

public class AppCenterIngestion implements Ingestion {

    /**
     * Default log URL.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_LOG_URL = "https://in.appcenter.ms";

    /**
     * API Path.
     */
    @VisibleForTesting
    static final String API_PATH = "/logs?api-version=1.0.0";

    /**
     * Installation identifier HTTP Header.
     */
    @VisibleForTesting
    static final String INSTALL_ID = "Install-ID";

    /**
     * Log serializer.
     */
    private final LogSerializer mLogSerializer;

    /**
     * HTTP client.
     */
    private final HttpClient mHttpClient;

    /**
     * Log base URL (scheme + authority).
     */
    private String mLogUrl;

    /**
     * Init.
     *
     * @param httpClient    the HTTP client instance.
     * @param logSerializer log serializer.
     */
    public AppCenterIngestion(@NonNull HttpClient httpClient, @NonNull LogSerializer logSerializer) {
        mLogSerializer = logSerializer;
        mHttpClient = httpClient;
        mLogUrl = DEFAULT_LOG_URL;
    }

    /**
     * Update log URL.
     *
     * @param logUrl log URL.
     */
    @Override
    @SuppressWarnings("SameParameterValue")
    public void setLogUrl(@NonNull String logUrl) {
        mLogUrl = logUrl;
    }

    @Override
    public ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, final ServiceCallback serviceCallback) throws IllegalArgumentException {
        Map<String, String> headers = new HashMap<>();
        headers.put(INSTALL_ID, installId.toString());
        headers.put(APP_SECRET, appSecret);
        HttpClient.CallTemplate callTemplate = new IngestionCallTemplate(mLogSerializer, logContainer);
        return mHttpClient.callAsync(mLogUrl + API_PATH, METHOD_POST, headers, callTemplate, serviceCallback);
    }

    @Override
    public void close() throws IOException {
        mHttpClient.close();
    }

    @Override
    public void reopen() {
        mHttpClient.reopen();
    }

    /**
     * Inner class is used to be able to mock System.currentTimeMillis, does not work if using anonymous inner class...
     */
    private static class IngestionCallTemplate extends AbstractAppCallTemplate {

        private final LogSerializer mLogSerializer;

        private final LogContainer mLogContainer;

        IngestionCallTemplate(LogSerializer logSerializer, LogContainer logContainer) {
            mLogSerializer = logSerializer;
            mLogContainer = logContainer;
        }

        @Override
        public String buildRequestBody() throws JSONException {

            /* Serialize payload. */
            return mLogSerializer.serializeContainer(mLogContainer);
        }
    }
}
