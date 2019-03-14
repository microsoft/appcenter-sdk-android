/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class DoubleTypedPropertyTest {

    @Test
    public void equalsHashCode() {
        DoubleTypedProperty a = new DoubleTypedProperty();
        DoubleTypedProperty b = new DoubleTypedProperty();
        checkNotEquals(a, null);
        checkNotEquals(null, a);
        checkNotEquals(a, new Object());
        checkEquals(a, a);
        checkEquals(a, b);
        a.setName("name");
        checkNotEquals(a, b);
        b.setName("name");
        checkEquals(a, b);
        a.setValue(100d);
        checkNotEquals(a, b);
        b.setValue(100d);
        checkEquals(a, b);
    }
}
