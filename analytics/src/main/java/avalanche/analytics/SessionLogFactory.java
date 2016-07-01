package avalanche.analytics;

import avalanche.analytics.ingestion.models.EndSessionLog;
import avalanche.base.ingestion.models.json.LogFactory;

public class SessionLogFactory implements LogFactory {

    @Override
    public EndSessionLog create() {
        return new EndSessionLog();
    }
}
