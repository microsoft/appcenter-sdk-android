/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.client;

import android.accounts.NetworkErrorException;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientDecorator;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;

import java.util.Map;

public class StorageHttpClientDecorator extends HttpClientDecorator {

    private boolean mOfflineModeEnabled;

    public StorageHttpClientDecorator(HttpClient decoratedApi) {
        super(decoratedApi);
    }

    /**
     * Make an HTTP call.
     *
     * @param url             URL.
     * @param method          GET or POST.
     * @param headers         headers, can be empty.
     * @param callTemplate    callbacks to provide request body or get notification before calling.
     * @param serviceCallback callbacks to monitor the completion of the HTTP call.
     * @return a call handle to later cancel the call.
     */
    @Override
    public synchronized ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback) {
        if (mOfflineModeEnabled) {
            serviceCallback.onCallFailed(new NetworkErrorException("Storage offline simulation mode is enabled"));
        } else {
            mDecoratedApi.callAsync(url, method, headers, callTemplate, serviceCallback);
        }
        return null;
    }
}
