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

        /* ID. */
        a.setId("a1");
        checkNotEquals(a, b);
        b.setId("b1");
        checkNotEquals(a, b);
        b.setId("a1");
        checkEquals(a, b);

        /* Local ID. */
        a.setLocalId("a1");
        checkNotEquals(a, b);
        b.setLocalId("b1");
        checkNotEquals(a, b);
        b.setLocalId("a1");
        checkEquals(a, b);

        /* Auth ID. */
        a.setAuthId("a1");
        checkNotEquals(a, b);
        b.setAuthId("b1");
        checkNotEquals(a, b);
        b.setAuthId("a1");
        checkEquals(a, b);

        /* Auth sec ID. */
        a.setAuthSecId("a1");
        checkNotEquals(a, b);
        b.setAuthSecId("b1");
        checkNotEquals(a, b);
        b.setAuthSecId("a1");
        checkEquals(a, b);
    }
}
