/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

public class MockCommonSchemaLog extends CommonSchemaLog {

    public static final String TYPE = "mock";

    @Override
    public String getType() {
        return TYPE;
    }
}
