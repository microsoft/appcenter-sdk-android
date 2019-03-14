/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.ingestion.models.one.json;

import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CommonSchemaEventLogFactoryTest {

    @Test
    public void createEvent() {
        CommonSchemaEventLog eventLog = new CommonSchemaEventLogFactory().create();
        assertNotNull(eventLog);
        assertEquals(CommonSchemaEventLog.TYPE, eventLog.getType());
    }
}
