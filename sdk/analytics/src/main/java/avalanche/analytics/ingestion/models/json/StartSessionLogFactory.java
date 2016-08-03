package avalanche.analytics.ingestion.models.json;

import avalanche.analytics.ingestion.models.StartSessionLog;
import avalanche.core.ingestion.models.json.LogFactory;

public class StartSessionLogFactory implements LogFactory {

    @Override
    public StartSessionLog create() {
        return new StartSessionLog();
    }
}
