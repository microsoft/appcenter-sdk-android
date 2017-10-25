package com.microsoft.appcenter.analytics.ingestion.models.json;

import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;

public class PageLogFactory implements LogFactory {

    @Override
    public PageLog create() {
        return new PageLog();
    }
}
