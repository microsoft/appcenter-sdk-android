/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class AbstractLogFactoryTest {

    @Test
    public void toCommonSchemaLogs() {
        AbstractLogFactory logFactory = new AbstractLogFactory() {

            @Override
            public Log create() {
                return null;
            }
        };
        Collection<CommonSchemaLog> logs = logFactory.toCommonSchemaLogs(null);
        assertNotNull(logs);
        assertSame(0, logs.size());
    }
}