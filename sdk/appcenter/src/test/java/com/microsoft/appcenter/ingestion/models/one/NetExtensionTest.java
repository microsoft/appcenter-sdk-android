/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class NetExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new NetExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        NetExtension a = new NetExtension();
        NetExtension b = new NetExtension();
        checkEquals(a, b);

        /* Provider. */
        a.setProvider("a1");
        checkNotEquals(a, b);
        b.setProvider("b1");
        checkNotEquals(a, b);
        b.setProvider("a1");
        checkEquals(a, b);
    }
}
