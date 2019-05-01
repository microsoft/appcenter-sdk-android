/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.ingestion.models;

import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

@SuppressWarnings("unused")
public class EventLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new EventLog());
    }

    @Test
    public void compareDevices() {

        /* Empty objects. */
        EventLog a = new EventLog();
        EventLog b = new EventLog();
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

        /* Id */
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        a.setId(sid1);
        checkNotEquals(a, b);
        b.setId(sid2);
        checkNotEquals(a, b);
        b.setId(sid1);
        checkEquals(a, b);

        /* Event properties. */
        a.setTypedProperties(new ArrayList<TypedProperty>());
        checkNotEquals(a, b);
        a.setTypedProperties(null);
        b.setTypedProperties(new ArrayList<TypedProperty>());
        checkNotEquals(a, b);
        a.setTypedProperties(new ArrayList<TypedProperty>());
        checkEquals(a, b);
    }
}
