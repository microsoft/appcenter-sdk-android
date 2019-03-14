/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class DeviceExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new DeviceExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        DeviceExtension a = new DeviceExtension();
        DeviceExtension b = new DeviceExtension();
        checkEquals(a, b);

        /* Local ID. */
        a.setLocalId("a1");
        checkNotEquals(a, b);
        b.setLocalId("b1");
        checkNotEquals(a, b);
        b.setLocalId("a1");
        checkEquals(a, b);
    }
}
