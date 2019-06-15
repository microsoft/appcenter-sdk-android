/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

@SuppressWarnings("unused")
public class CustomPropertiesLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new CustomPropertiesLog());
    }

    @Test
    public void compare() {

        /* Empty objects. */
        CustomPropertiesLog a = new CustomPropertiesLog();
        CustomPropertiesLog b = new CustomPropertiesLog();
        checkEquals(a, b);
        checkEquals(a.getType(), CustomPropertiesLog.TYPE);

        UUID sid = UUID.randomUUID();
        a.setSid(sid);
        checkNotEquals(a, b);
        b.setSid(sid);
        checkEquals(a, b);

        /* Properties. */
        Map<String, Object> properties = new HashMap<>();
        properties.put("test", "test");
        a.setProperties(properties);
        checkEquals(a.getProperties(), properties);
        checkNotEquals(a, b);
        b.setProperties(new HashMap<String, Object>());
        checkNotEquals(a, b);
        b.setProperties(properties);
        checkEquals(a, b);
    }
}