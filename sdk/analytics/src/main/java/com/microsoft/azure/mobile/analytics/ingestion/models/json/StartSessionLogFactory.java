package com.microsoft.azure.mobile.analytics.ingestion.models.json;

import com.microsoft.azure.mobile.analytics.ingestion.models.StartSessionLog;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

public class StartSessionLogFactory implements LogFactory {

    @Override
    public StartSessionLog create() {
        return new StartSessionLog();
    }
}
