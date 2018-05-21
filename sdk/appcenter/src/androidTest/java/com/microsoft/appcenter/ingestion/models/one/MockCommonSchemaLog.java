package com.microsoft.appcenter.ingestion.models.one;

class MockCommonSchemaLog extends CommonSchemaLog {

    static final String TYPE = "mock";

    @Override
    public String getType() {
        return TYPE;
    }
}
