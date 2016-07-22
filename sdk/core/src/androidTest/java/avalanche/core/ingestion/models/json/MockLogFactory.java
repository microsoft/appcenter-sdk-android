package avalanche.core.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.Log;

public class MockLogFactory implements LogFactory, ModelFactory<Log> {

    @Override
    public Log create() {
        return new MockLog();
    }

    @Override
    public List<Log> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
