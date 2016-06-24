package avalanche.analytics;

import avalanche.analytics.ingestion.models.PageLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.json.LogFactory;

public class PageLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new PageLog();
    }
}
