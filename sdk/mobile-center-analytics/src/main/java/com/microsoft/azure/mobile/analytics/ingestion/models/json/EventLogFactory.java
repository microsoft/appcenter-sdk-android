package com.microsoft.azure.mobile.analytics.ingestion.models.json;

import com.microsoft.azure.mobile.analytics.ingestion.models.EventLog;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

public class EventLogFactory implements LogFactory {

    @Override
    public EventLog create() {
        return new EventLog();
    }
}
