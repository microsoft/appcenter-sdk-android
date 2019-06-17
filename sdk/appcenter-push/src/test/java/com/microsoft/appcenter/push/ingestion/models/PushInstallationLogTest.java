/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push.ingestion.models;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

@SuppressWarnings("unused")
public class PushInstallationLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new PushInstallationLog());
    }

    @Test
    public void compareDevices() {

        /* Empty objects. */
        PushInstallationLog a = new PushInstallationLog();
        PushInstallationLog b = new PushInstallationLog();
        checkEquals(a, b);
        checkEquals(a.getType(), PushInstallationLog.TYPE);

        UUID sid = UUID.randomUUID();
        a.setSid(sid);
        checkNotEquals(a, b);
        b.setSid(sid);
        checkEquals(a, b);

        /* PushToken. */
        a.setPushToken("a");
        checkEquals(a.getPushToken(), "a");
        checkNotEquals(a, b);
        b.setPushToken("b");
        checkNotEquals(a, b);
        b.setPushToken("a");
        checkEquals(a, b);
    }
}