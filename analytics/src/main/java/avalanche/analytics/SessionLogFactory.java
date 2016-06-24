package avalanche.analytics;

import avalanche.analytics.ingestion.models.SessionLog;
import avalanche.base.ingestion.models.json.LogFactory;

public class SessionLogFactory implements LogFactory {

    @Override
    public SessionLog create() {
        return new SessionLog();
    }
}
