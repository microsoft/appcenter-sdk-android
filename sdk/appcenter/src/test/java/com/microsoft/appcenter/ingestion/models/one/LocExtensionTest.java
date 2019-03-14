/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class LocExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new LocExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        LocExtension a = new LocExtension();
        LocExtension b = new LocExtension();
        checkEquals(a, b);

        /* Tz. */
        a.setTz("a1");
        checkNotEquals(a, b);
        b.setTz("b1");
        checkNotEquals(a, b);
        b.setTz("a1");
        checkEquals(a, b);
    }
}
