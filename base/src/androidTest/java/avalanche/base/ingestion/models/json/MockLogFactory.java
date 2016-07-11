package avalanche.base.ingestion.models.json;

import avalanche.base.ingestion.models.Log;

public class MockLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new MockLog();
    }
}
