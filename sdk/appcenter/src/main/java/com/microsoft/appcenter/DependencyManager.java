/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import com.microsoft.appcenter.http.HttpClient;

@SuppressWarnings("WeakerAccess")
public class DependencyManager {

    /**
     * A wrapper SDK can use this method to inject HttpClient.
     *
     * @param httpClient An HTTP client instance.
     */
    public static void setDependencies(HttpClient httpClient) {
        AppCenter.getInstance().setHttpClient(httpClient);
    }
}
