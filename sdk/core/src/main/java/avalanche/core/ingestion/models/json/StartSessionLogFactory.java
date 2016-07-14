package avalanche.core.ingestion.models.json;

import avalanche.core.ingestion.models.StartSessionLog;

public class StartSessionLogFactory implements LogFactory {

    @Override
    public StartSessionLog create() {
        return new StartSessionLog();
    }
}
