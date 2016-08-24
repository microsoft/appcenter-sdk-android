package com.microsoft.sonoma.analytics.ingestion.models.json;

import com.microsoft.sonoma.analytics.ingestion.models.PageLog;
import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;

public class PageLogFactory implements LogFactory {

    @Override
    public PageLog create() {
        return new PageLog();
    }
}
