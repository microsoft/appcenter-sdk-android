package com.microsoft.appcenter.ingestion.models.one;

class MockCommonSchemaLog extends CommonSchemaLog {

    private static final String TYPE = "mock";

    @Override
    public String getType() {
        return TYPE;
    }
}
