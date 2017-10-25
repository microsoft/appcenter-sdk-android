package com.microsoft.appcenter.http;

import java.util.Map;

/**
 * Helper class used to share logic with multiple decorators.
 */
abstract class HttpClientCallDecorator implements Runnable, ServiceCall, ServiceCallback {

    /**
     * Decorated API.
     */
    final HttpClient mDecoratedApi;

    final String mUrl;

    final String mMethod;

    final Map<String, String> mHeaders;

    final HttpClient.CallTemplate mCallTemplate;

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
    public void onCallSucceeded(String payload) {
        mServiceCallback.onCallSucceeded(payload);
    }
}
