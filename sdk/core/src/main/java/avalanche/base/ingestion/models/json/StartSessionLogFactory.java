package avalanche.base.ingestion.models.json;

import avalanche.base.ingestion.models.StartSessionLog;

public class StartSessionLogFactory implements LogFactory {

    @Override
    public StartSessionLog create() {
        return new StartSessionLog();
    }
}
