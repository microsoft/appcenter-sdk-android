package com.microsoft.appcenter.analytics.ingestion.models.json;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;

public class EventLogFactory implements LogFactory {

    @Override
    public EventLog create() {
        return new EventLog();
    }
}
