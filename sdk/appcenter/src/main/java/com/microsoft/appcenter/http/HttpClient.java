/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import org.json.JSONException;

import java.io.Closeable;
import java.net.URL;
import java.util.Map;

/**
 * HTTP client abstraction.
 */
public interface HttpClient extends Closeable {

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
    ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback);

    /**
     * Call callbacks.
     */
    interface CallTemplate {

        /**
         * Called when the method is POST to provide request body.
         *
         * @return request body.
         * @throws JSONException callback can throw this to make the call fail if a JSON error occurs.
         */
        String buildRequestBody() throws JSONException;

        /**
         * Called before the call is made.
         *
         * @param url     url.
         * @param headers headers.
         */
        void onBeforeCalling(URL url, Map<String, String> headers);
    }

    /**
     * Make this client active again after closing.
     */
    void reopen();
}
