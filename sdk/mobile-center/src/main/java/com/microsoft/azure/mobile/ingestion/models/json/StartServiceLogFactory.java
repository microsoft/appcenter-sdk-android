package com.microsoft.azure.mobile.ingestion.models.json;

import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.StartServiceLog;

public class StartServiceLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new StartServiceLog();
    }
}
