/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import java.util.Map;

/**
 * Helper class used to share logic with multiple decorators.
 */
abstract class HttpClientCallDecorator implements Runnable, ServiceCall, ServiceCallback {

    /**
     * Decorated API.
     */
    private final HttpClient mDecoratedApi;

    private final String mUrl;

    private final String mMethod;

    private final Map<String, String> mHeaders;

    private final HttpClient.CallTemplate mCallTemplate;

    /**
     * Callback.
     */
    final ServiceCallback mServiceCallback;

    /**
     * Call.
     */
    ServiceCall mServiceCall;

    HttpClientCallDecorator(HttpClient decoratedApi, String url, String method, Map<String, String> headers, HttpClient.CallTemplate callTemplate, ServiceCallback serviceCallback) {
        mDecoratedApi = decoratedApi;
        mUrl = url;
        mMethod = method;
        mHeaders = headers;
        mCallTemplate = callTemplate;
        mServiceCallback = serviceCallback;
    }

    @Override
    public synchronized void cancel() {
        mServiceCall.cancel();
    }

    @Override
    public synchronized void run() {
        mServiceCall = mDecoratedApi.callAsync(mUrl, mMethod, mHeaders, mCallTemplate, this);
    }

    @Override
    public void onCallSucceeded(String payload, Map<String, String> headers) {
        mServiceCallback.onCallSucceeded(payload, headers);
    }

    @Override
    public void onCallFailed(Exception e) {
        mServiceCallback.onCallFailed(e);
    }
}
