/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.LogWithProperties;

public class MockLog extends LogWithProperties {

    public static final String MOCK_LOG_TYPE = "mockLog";

    @Override
    public String getType() {
        return MOCK_LOG_TYPE;
    }
}
