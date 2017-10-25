package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.CustomPropertiesLog;
import com.microsoft.appcenter.ingestion.models.Log;

public class CustomPropertiesLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new CustomPropertiesLog();
    }
}

