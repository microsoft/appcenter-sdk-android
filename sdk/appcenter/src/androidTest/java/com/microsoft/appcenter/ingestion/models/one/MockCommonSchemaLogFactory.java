package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;

class MockCommonSchemaLogFactory implements LogFactory {

    @Override
    public Log create() {
        return new MockCommonSchemaLog();
    }
}
