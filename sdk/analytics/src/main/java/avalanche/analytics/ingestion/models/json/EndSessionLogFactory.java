package avalanche.analytics.ingestion.models.json;

import avalanche.analytics.ingestion.models.EndSessionLog;
import avalanche.base.ingestion.models.json.LogFactory;

public class EndSessionLogFactory implements LogFactory {

    @Override
    public EndSessionLog create() {
        return new EndSessionLog();
    }
}
