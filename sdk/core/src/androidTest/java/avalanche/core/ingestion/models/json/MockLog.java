package avalanche.core.ingestion.models.json;

import avalanche.core.ingestion.models.LogWithProperties;

public class MockLog extends LogWithProperties {

    public static final String MOCK_LOG_TYPE = "mockLog";

    @Override
    public String getType() {
        return MOCK_LOG_TYPE;
    }
}
