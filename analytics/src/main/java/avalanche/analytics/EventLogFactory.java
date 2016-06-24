package avalanche.analytics;

import avalanche.analytics.ingestion.models.EventLog;
import avalanche.base.ingestion.models.json.LogFactory;

public class EventLogFactory implements LogFactory {

    @Override
    public EventLog create() {
        return new EventLog();
    }
}
