package com.microsoft.azure.mobile.push.ingestion.models;

import com.microsoft.azure.mobile.test.TestUtils;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import org.junit.Test;

import java.util.UUID;

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
        checkEquals(a.getType(), PushInstallationLog.TYPE);

        UUID sid = UUIDUtils.randomUUID();
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