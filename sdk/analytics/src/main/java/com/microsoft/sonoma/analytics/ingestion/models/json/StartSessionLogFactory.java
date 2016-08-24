package com.microsoft.sonoma.analytics.ingestion.models.json;

import com.microsoft.sonoma.analytics.ingestion.models.StartSessionLog;
import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;

public class StartSessionLogFactory implements LogFactory {

    @Override
    public StartSessionLog create() {
        return new StartSessionLog();
    }
}
