package avalanche.core.ingestion.models;

import org.junit.Test;

import avalanche.core.utils.TestUtils;

import static avalanche.core.utils.TestUtils.checkEquals;
import static avalanche.core.utils.TestUtils.checkNotEquals;

public class DeviceTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new Device());
    }

    @Test
    public void compareDevices() {

        /* Empty objects. */
        Device a = new Device();
        Device b = new Device();
        checkEquals(a, b);

        /* Sdk version. */
        a.setSdkVersion("a");
        checkNotEquals(a, b);
        a.setSdkVersion(null);
        b.setSdkVersion("a");
        checkNotEquals(a, b);
        a.setSdkVersion("b");
        checkNotEquals(a, b);
        a.setSdkVersion("a");
        checkEquals(a, b);

        /* Model. */
        a.setModel("a");
        checkNotEquals(a, b);
        a.setModel(null);
        b.setModel("a");
        checkNotEquals(a, b);
        a.setModel("b");
        checkNotEquals(a, b);
        a.setModel("a");
        checkEquals(a, b);

        /* OEM name. */
        a.setOemName("a");
        checkNotEquals(a, b);
        a.setOemName(null);
        b.setOemName("a");
        checkNotEquals(a, b);
        a.setOemName("b");
        checkNotEquals(a, b);
        a.setOemName("a");
        checkEquals(a, b);

        /* OS name. */
        a.setOsName("a");
        checkNotEquals(a, b);
        a.setOsName(null);
        b.setOsName("a");
        checkNotEquals(a, b);
        a.setOsName("b");
        checkNotEquals(a, b);
        a.setOsName("a");
        checkEquals(a, b);

        /* OS version. */
        a.setOsVersion("a");
        checkNotEquals(a, b);
        a.setOsVersion(null);
        b.setOsVersion("a");
        checkNotEquals(a, b);
        a.setOsVersion("b");
        checkNotEquals(a, b);
        a.setOsVersion("a");
        checkEquals(a, b);

        /* OS API level. */
        a.setOsApiLevel(1);
        checkNotEquals(a, b);
        a.setOsApiLevel(null);
        b.setOsApiLevel(1);
        checkNotEquals(a, b);
        a.setOsApiLevel(2);
        checkNotEquals(a, b);
        a.setOsApiLevel(1);
        checkEquals(a, b);

        /* Locale. */
        a.setLocale("a");
        checkNotEquals(a, b);
        a.setLocale(null);
        b.setLocale("a");
        checkNotEquals(a, b);
        a.setLocale("b");
        checkNotEquals(a, b);
        a.setLocale("a");
        checkEquals(a, b);

        /* Time zone offset. */
        a.setTimeZoneOffset(1);
        checkNotEquals(a, b);
        a.setTimeZoneOffset(null);
        b.setTimeZoneOffset(1);
        checkNotEquals(a, b);
        a.setTimeZoneOffset(2);
        checkNotEquals(a, b);
        a.setTimeZoneOffset(1);
        checkEquals(a, b);

        /* Screen size. */
        a.setScreenSize("a");
        checkNotEquals(a, b);
        a.setScreenSize(null);
        b.setScreenSize("a");
        checkNotEquals(a, b);
        a.setScreenSize("b");
        checkNotEquals(a, b);
        a.setScreenSize("a");
        checkEquals(a, b);

        /* App version. */
        a.setAppVersion("a");
        checkNotEquals(a, b);
        a.setAppVersion(null);
        b.setAppVersion("a");
        checkNotEquals(a, b);
        a.setAppVersion("b");
        checkNotEquals(a, b);
        a.setAppVersion("a");
        checkEquals(a, b);

        /* Carrier name. */
        a.setCarrierName("a");
        checkNotEquals(a, b);
        a.setCarrierName(null);
        b.setCarrierName("a");
        checkNotEquals(a, b);
        a.setCarrierName("b");
        checkNotEquals(a, b);
        a.setCarrierName("a");
        checkEquals(a, b);

        /* Carrier country. */
        a.setCarrierCountry("a");
        checkNotEquals(a, b);
        a.setCarrierCountry(null);
        b.setCarrierCountry("a");
        checkNotEquals(a, b);
        a.setCarrierCountry("b");
        checkNotEquals(a, b);
        a.setCarrierCountry("a");
        checkEquals(a, b);

        /* App build. */
        a.setAppBuild("a");
        checkNotEquals(a, b);
        a.setAppBuild(null);
        b.setAppBuild("a");
        checkNotEquals(a, b);
        a.setAppBuild("b");
        checkNotEquals(a, b);
        a.setAppBuild("a");
        checkEquals(a, b);

        /* App namespace. */
        a.setAppNamespace("a");
        checkNotEquals(a, b);
        a.setAppNamespace(null);
        b.setAppNamespace("a");
        checkNotEquals(a, b);
        a.setAppNamespace("b");
        checkNotEquals(a, b);
        a.setAppNamespace("a");
        checkEquals(a, b);
    }
}
