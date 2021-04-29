/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.ingestion;

import androidx.annotation.NonNull;

import com.microsoft.appcenter.ingestion.AbstractAppCenterIngestion;
import com.microsoft.appcenter.http.HttpClient;

public class DistributeIngestion extends AbstractAppCenterIngestion {

    public DistributeIngestion(@NonNull HttpClient httpClient) {
        super(httpClient);
    }
}
