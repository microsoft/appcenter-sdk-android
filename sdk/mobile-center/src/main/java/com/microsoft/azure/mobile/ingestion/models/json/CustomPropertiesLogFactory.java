package com.microsoft.azure.mobile.ingestion.models.json;

import com.microsoft.azure.mobile.ingestion.models.CustomPropertiesLog;
import com.microsoft.azure.mobile.ingestion.models.Log;

public class CustomPropertiesLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new CustomPropertiesLog();
    }
}

