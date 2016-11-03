package com.microsoft.azure.mobile.ingestion.models.json;

import com.microsoft.azure.mobile.ingestion.models.LogWithProperties;

public class MockLog extends LogWithProperties {

    public static final String MOCK_LOG_TYPE = "mockLog";

    @Override
    public String getType() {
        return MOCK_LOG_TYPE;
    }
}
