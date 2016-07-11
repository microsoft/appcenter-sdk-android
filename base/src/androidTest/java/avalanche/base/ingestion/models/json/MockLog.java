package avalanche.base.ingestion.models.json;

import avalanche.base.ingestion.models.LogWithProperties;

public class MockLog extends LogWithProperties {

    public static final String MOCK_LOG_TYPE = "mockLog";

    @Override
    public String getType() {
        return MOCK_LOG_TYPE;
    }
}
