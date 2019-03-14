/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.AbstractLog;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

@SuppressWarnings("unused")
public class LogContainerTest {

    @Test
    public void compareLogContainer() {
        LogContainer container1 = new LogContainer();
        LogContainer container2 = new LogContainer();
        TestUtils.compareSelfNullClass(container1);
        TestUtils.checkEquals(container1, container2);

        Log log1 = new AbstractLog() {
            @Override
            public String getType() {
                return "null";
            }
        };
        log1.setSid(UUID.randomUUID());

        container1.setLogs(Collections.singletonList(log1));
        TestUtils.compareSelfNullClass(container1);
        TestUtils.checkNotEquals(container1, container2);

        container2.setLogs(Collections.singletonList(log1));
        TestUtils.compareSelfNullClass(container1);
        TestUtils.checkEquals(container1, container2);

        Log log2 = new AbstractLog() {
            @Override
            public String getType() {
                return null;
            }
        };
        log2.setSid(UUID.randomUUID());

        container2.setLogs(Collections.singletonList(log2));
        TestUtils.compareSelfNullClass(container1);
        TestUtils.checkNotEquals(container1, container2);
    }
}
