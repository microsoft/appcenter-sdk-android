package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class MockCommonSchemaLogFactory extends AbstractLogFactory {

    @Override
    public Log create() {
        return new MockCommonSchemaLog();
    }
}
