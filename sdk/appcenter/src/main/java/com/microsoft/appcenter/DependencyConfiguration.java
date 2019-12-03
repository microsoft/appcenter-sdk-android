/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import com.microsoft.appcenter.http.HttpClient;

/**
 * Configuration to override default dependencies used by the SDK.
 */
public final class DependencyConfiguration {

    /**
     * HTTP client.
     */
    private static HttpClient sHttpClient;

    DependencyConfiguration() {
    }

    /**
     * Get HTTP client.
     *
     * @return HTTP client.
     */
    public static HttpClient getHttpClient() {
        return sHttpClient;
    }

    /**
     * Set HTTP client.
     *
     * @param httpClient HTTP client.
     */
    public static void setHttpClient(HttpClient httpClient) {
        sHttpClient = httpClient;
    }
}
