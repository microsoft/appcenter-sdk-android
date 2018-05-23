package com.microsoft.appcenter.analytics.ingestion.models.json;

import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class StartSessionLogFactory extends AbstractLogFactory {

    @Override
    public StartSessionLog create() {
        return new StartSessionLog();
    }
}
