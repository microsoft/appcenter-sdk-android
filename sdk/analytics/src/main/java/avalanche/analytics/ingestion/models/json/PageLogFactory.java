package avalanche.analytics.ingestion.models.json;

import avalanche.analytics.ingestion.models.PageLog;
import avalanche.core.ingestion.models.json.LogFactory;

public class PageLogFactory implements LogFactory {

    @Override
    public PageLog create() {
        return new PageLog();
    }
}
