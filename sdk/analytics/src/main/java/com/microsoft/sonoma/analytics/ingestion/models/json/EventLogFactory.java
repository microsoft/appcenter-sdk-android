package com.microsoft.sonoma.analytics.ingestion.models.json;

import com.microsoft.sonoma.analytics.ingestion.models.EventLog;
import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;

public class EventLogFactory implements LogFactory {

    @Override
    public EventLog create() {
        return new EventLog();
    }
}
