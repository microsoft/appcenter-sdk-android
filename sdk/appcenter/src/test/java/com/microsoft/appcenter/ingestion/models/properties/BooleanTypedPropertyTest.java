/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class BooleanTypedPropertyTest {

    @Test
    public void equalsHashCode() {
        BooleanTypedProperty a = new BooleanTypedProperty();
        BooleanTypedProperty b = new BooleanTypedProperty();
        checkNotEquals(a, null);
        checkNotEquals(null, a);
        checkNotEquals(a, new Object());
        checkEquals(a, a);
        checkEquals(a, b);
        a.setName("name");
        checkNotEquals(a, b);
        b.setName("name");
        checkEquals(a, b);
        a.setValue(true);
        checkNotEquals(a, b);
        b.setValue(true);
        checkEquals(a, b);
    }
}
