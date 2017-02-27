package com.microsoft.azure.mobile.push.ingestion.models;

import com.microsoft.azure.mobile.test.TestUtils;

import org.junit.Test;

import static com.microsoft.azure.mobile.test.TestUtils.checkEquals;
import static com.microsoft.azure.mobile.test.TestUtils.checkNotEquals;

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

        /* PushToken. */
        a.setPushToken("a");
        checkNotEquals(a, b);
        b.setPushToken("b");
        checkNotEquals(a, b);
        b.setPushToken("a");
        checkEquals(a, b);
    }
}