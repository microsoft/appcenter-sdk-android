/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import org.junit.Test;

import java.util.Date;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class DateTimeTypedPropertyTest {

    @Test
    public void equalsHashCode() {
        DateTimeTypedProperty a = new DateTimeTypedProperty();
        DateTimeTypedProperty b = new DateTimeTypedProperty();
        Date date = new Date(100);
        checkNotEquals(a, null);
        checkNotEquals(null, a);
        checkNotEquals(a, new Object());
        checkEquals(a, a);
        checkEquals(a, b);
        a.setName("name");
        checkNotEquals(a, b);
        b.setName("name");
        checkEquals(a, b);
        a.setValue(date);
        checkNotEquals(a, b);
        b.setValue(new Date());
        checkNotEquals(a, b);
        b.setValue(date);
        checkEquals(a, b);
    }
}
