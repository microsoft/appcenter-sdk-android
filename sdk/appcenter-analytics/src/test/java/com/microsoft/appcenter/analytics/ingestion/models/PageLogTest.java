/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.ingestion.models;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

@SuppressWarnings("unused")
public class PageLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new PageLog());
    }

    @Test
    public void compareDevices() {

        /* Empty objects. */
        PageLog a = new PageLog();
        PageLog b = new PageLog();
        checkEquals(a, b);

        /* Properties. */
        Map<String, String> p1 = new HashMap<>();
        p1.put("a", "b");
        Map<String, String> p2 = new HashMap<>();
        p1.put("c", "d");
        a.setProperties(p1);
        checkNotEquals(a, b);
        b.setProperties(p2);
        checkNotEquals(a, b);
        b.setProperties(p1);
        checkEquals(a, b);

        /* Name. */
        a.setName("a");
        checkNotEquals(a, b);
        b.setName("b");
        checkNotEquals(a, b);
        b.setName("a");
        checkEquals(a, b);
    }
}
