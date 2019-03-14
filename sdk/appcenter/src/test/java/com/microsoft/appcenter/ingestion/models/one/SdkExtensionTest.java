/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class SdkExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new SdkExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        SdkExtension a = new SdkExtension();
        SdkExtension b = new SdkExtension();
        checkEquals(a, b);

        /* libVer. */
        a.setLibVer("a1");
        checkNotEquals(a, b);
        b.setLibVer("b1");
        checkNotEquals(a, b);
        b.setLibVer("a1");
        checkEquals(a, b);

        /* Epoch. */
        a.setEpoch("a2");
        checkNotEquals(a, b);
        b.setEpoch("b2");
        checkNotEquals(a, b);
        b.setEpoch("a2");
        checkEquals(a, b);

        /* Seq. */
        a.setSeq(0L);
        checkNotEquals(a, b);
        b.setSeq(1L);
        checkNotEquals(a, b);
        b.setSeq(0L);
        checkEquals(a, b);

        /* InstallId. */
        a.setInstallId(UUID.randomUUID());
        checkNotEquals(a, b);
        b.setInstallId(UUID.randomUUID());
        checkNotEquals(a, b);
        b.setInstallId(a.getInstallId());
        checkEquals(a, b);
    }
}
