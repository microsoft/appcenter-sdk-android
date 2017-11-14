package com.microsoft.appcenter.analytics.ingestion.models.json;

import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;

public class StartSessionLogFactory implements LogFactory {

    @Override
    public StartSessionLog create() {
        return new StartSessionLog();
    }
}
