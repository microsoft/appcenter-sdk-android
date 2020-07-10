/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.ingestion.models.json;

import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DistributionStartSessionLogFactoryTest {

    @Test
    public void DistributionStartSessionLogFactoryCreateTest() {
        DistributionStartSessionLogFactory sessionLogFactory = new DistributionStartSessionLogFactory();
        DistributionStartSessionLog sessionLog = sessionLogFactory.create();
        assertNotNull(sessionLog);
    }
}
