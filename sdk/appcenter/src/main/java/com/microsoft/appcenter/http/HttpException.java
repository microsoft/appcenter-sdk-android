/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP exception.
 */
public class HttpException extends IOException {

    /**
     * HTTP status code.
     */
    private final int statusCode;

    /**
     * HTTP payload.
     */
    private final String payload;

    /**
     * HTTP headers.
     */
    private final Map<String, String> headers;

    /**
     * Init with empty response body.
     *
     * @param status HTTP status code.
     */
    public HttpException(int status) {
        this(status, "");
    }

    /**
     * Init.
     *
     * @param status  HTTP status code.
     * @param payload HTTP payload.
     */
    public HttpException(int status, @NonNull String payload) {
        this(status, payload, new HashMap<String, String>());
    }

    /**
     * Init.
     *
     * @param status  HTTP status code.
     * @param payload HTTP payload.
     * @param headers HTTP responseHeaders.
     */
    public HttpException(int status, @NonNull String payload, @NonNull Map<String, String> headers) {
        super(getDetailMessage(status, payload));
        this.payload = payload;
        this.statusCode = status;
        this.headers = headers;
    }

    @NonNull
    private static String getDetailMessage(int status, @NonNull String payload) {
        if (TextUtils.isEmpty(payload)) {
            return String.valueOf(status);
        }
        return status + " - " + payload;
    }

    /**
     * Get the HTTP status code.
     *
     * @return HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the HTTP payload (response body).
     *
     * @return HTTP payload. Can be empty string.
     */
    @NonNull
    public String getPayload() {
        return payload;
    }

    /**
     * Get the HTTP headers (response headers).
     *
     * @return HTTP headers.
     */
    @NonNull
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HttpException that = (HttpException) o;

        return (statusCode == that.statusCode && payload.equals(that.payload) && headers.equals(that.headers));
    }

    @Override
    public int hashCode() {
        int result = statusCode;
        result = 31 * result + payload.hashCode();
        result = 31 * result + headers.hashCode();
        return result;
    }
}