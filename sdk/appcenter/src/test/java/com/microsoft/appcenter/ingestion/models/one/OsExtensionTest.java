/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class OsExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new OsExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        OsExtension a = new OsExtension();
        OsExtension b = new OsExtension();
        checkEquals(a, b);

        /* name. */
        a.setName("a1");
        checkNotEquals(a, b);
        b.setName("b1");
        checkNotEquals(a, b);
        b.setName("a1");
        checkEquals(a, b);

        /* Ver. */
        a.setVer("a2");
        checkNotEquals(a, b);
        b.setVer("b2");
        checkNotEquals(a, b);
        b.setVer("a2");
        checkEquals(a, b);
    }
}
