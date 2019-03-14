/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MockLogFactory implements LogFactory, ModelFactory<Log> {

    @Override
    public Log create() {
        return new MockLog();
    }

    @Override
    public Collection<CommonSchemaLog> toCommonSchemaLogs(Log log) {
        return null;
    }

    @Override
    public List<Log> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
