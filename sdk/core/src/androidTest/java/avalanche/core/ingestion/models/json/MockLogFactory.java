package avalanche.core.ingestion.models.json;

import avalanche.core.ingestion.models.Log;

public class MockLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new MockLog();
    }
}
