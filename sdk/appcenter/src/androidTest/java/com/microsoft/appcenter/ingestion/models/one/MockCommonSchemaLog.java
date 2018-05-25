package com.microsoft.appcenter.ingestion.models.one;

public class MockCommonSchemaLog extends CommonSchemaLog {

    public static final String TYPE = "mock";

    @Override
    public String getType() {
        return TYPE;
    }
}
