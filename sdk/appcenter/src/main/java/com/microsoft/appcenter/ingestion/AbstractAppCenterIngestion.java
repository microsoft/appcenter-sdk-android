/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract class to send logs to the Ingestion service.
 */
public abstract class AbstractAppCenterIngestion implements Ingestion {

    /**
     * Log base URL (scheme + authority).
     */
    private String mLogUrl;

    /**
     * HTTP client.
     */
    private HttpClient mHttpClient;

    public AbstractAppCenterIngestion() {
    }

    public AbstractAppCenterIngestion(HttpClient httpClient, String logUrl) {
        mLogUrl = logUrl;
        mHttpClient = httpClient;
    }

    @Override
    public ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void setLogUrl(String logUrl) {
        mLogUrl = logUrl;
    }

    public String getLogUrl() {
        return mLogUrl;
    }

    @Override
    public boolean isEnabled() {
        return SharedPreferencesManager.getBoolean(PrefStorageConstants.ALLOWED_NETWORK_REQUEST, true);
    }

    @Override
    public void reopen() {
        mHttpClient.reopen();
    }

    @Override
    public void close() throws IOException {
        mHttpClient.close();
    }

    public void setHttpClient(HttpClient httpClient) {
        mHttpClient = httpClient;
    }

    public ServiceCall getServiceCall(String url, String method, Map<String, String> headers, HttpClient.CallTemplate callTemplate, ServiceCallback serviceCallback) {
        if (!isEnabled()) {
            serviceCallback.onCallFailed(new ConnectException("SDK is in offline mode."));
            return null;
        }
        return mHttpClient.callAsync(url, method, headers, callTemplate, serviceCallback);
    }
}
