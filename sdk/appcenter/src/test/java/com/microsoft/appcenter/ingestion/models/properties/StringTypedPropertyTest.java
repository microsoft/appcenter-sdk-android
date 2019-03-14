/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class StringTypedPropertyTest {

    @Test
    public void equalsHashCode() {
        StringTypedProperty a = new StringTypedProperty();
        StringTypedProperty b = new StringTypedProperty();
        checkNotEquals(a, null);
        checkNotEquals(null, a);
        checkNotEquals(a, new Object());
        checkEquals(a, a);
        checkEquals(a, b);
        a.setName("name");
        checkNotEquals(a, b);
        b.setName("name");
        checkEquals(a, b);
        a.setValue("value1");
        checkNotEquals(a, b);
        b.setValue("value2");
        checkNotEquals(a, b);
        b.setValue("value1");
        checkEquals(a, b);
    }
}
