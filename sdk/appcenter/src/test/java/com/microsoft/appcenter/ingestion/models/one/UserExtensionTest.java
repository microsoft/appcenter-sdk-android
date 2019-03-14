/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class UserExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new UserExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        UserExtension a = new UserExtension();
        UserExtension b = new UserExtension();
        checkEquals(a, b);

        /* Locale. */
        a.setLocale("a1");
        checkNotEquals(a, b);
        b.setLocale("b1");
        checkNotEquals(a, b);
        b.setLocale("a1");
        checkEquals(a, b);

        /* LocalId */
        a.setLocalId("id-a");
        checkNotEquals(a, b);
        b.setLocalId("id-b");
        checkNotEquals(a, b);
        b.setLocalId("id-a");
        checkEquals(a, b);
    }
}
