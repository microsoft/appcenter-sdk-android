/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;
import static com.microsoft.appcenter.test.TestUtils.compareSelfNullClass;

@SuppressWarnings("unused")
public class DeviceTest {

    @Test
    public void compareDifferentType() {
        compareSelfNullClass(new Device());
        compareSelfNullClass(new WrapperSdk());
    }

    @Test
    public void compareDevices() {

        /* Empty objects. */
        Device a = new Device();
        Device b = new Device();
        checkEquals(a, b);

        /* Wrapper SDK information. */
        {
            /* Wrapper SDK version. */
            a.setWrapperSdkVersion("a");
            checkNotEquals(a, b);
            b.setWrapperSdkVersion("b");
            checkNotEquals(a, b);
            b.setWrapperSdkVersion("a");
            checkEquals(a, b);

            /* Wrapper SDK name. */
            a.setWrapperSdkName("a");
            checkNotEquals(a, b);
            b.setWrapperSdkName("b");
            checkNotEquals(a, b);
            b.setWrapperSdkName("a");
            checkEquals(a, b);

            /* Wrapper runtime version. */
            a.setWrapperRuntimeVersion("a");
            checkNotEquals(a, b);
            b.setWrapperRuntimeVersion("b");
            checkNotEquals(a, b);
            b.setWrapperRuntimeVersion("a");
            checkEquals(a, b);

            /* Live update release label. */
            a.setLiveUpdateReleaseLabel("a");
            checkNotEquals(a, b);
            b.setLiveUpdateReleaseLabel("b");
            checkNotEquals(a, b);
            b.setLiveUpdateReleaseLabel("a");
            checkEquals(a, b);

            /* Live update deployment key. */
            a.setLiveUpdateDeploymentKey("a");
            checkNotEquals(a, b);
            b.setLiveUpdateDeploymentKey("b");
            checkNotEquals(a, b);
            b.setLiveUpdateDeploymentKey("a");
            checkEquals(a, b);

            /* Live update package hash. */
            a.setLiveUpdatePackageHash("a");
            checkNotEquals(a, b);
            b.setLiveUpdatePackageHash("b");
            checkNotEquals(a, b);
            b.setLiveUpdatePackageHash("a");
            checkEquals(a, b);
        }

        /* Sdk name. */
        a.setSdkName("a");
        checkNotEquals(a, b);
        b.setSdkName("b");
        checkNotEquals(a, b);
        b.setSdkName("a");
        checkEquals(a, b);

        /* Sdk version. */
        a.setSdkVersion("a");
        checkNotEquals(a, b);
        b.setSdkVersion("b");
        checkNotEquals(a, b);
        b.setSdkVersion("a");
        checkEquals(a, b);

        /* Model. */
        a.setModel("a");
        checkNotEquals(a, b);
        b.setModel("b");
        checkNotEquals(a, b);
        b.setModel("a");
        checkEquals(a, b);

        /* OEM name. */
        a.setOemName("a");
        checkNotEquals(a, b);
        b.setOemName("b");
        checkNotEquals(a, b);
        b.setOemName("a");
        checkEquals(a, b);

        /* OS name. */
        a.setOsName("a");
        checkNotEquals(a, b);
        b.setOsName("b");
        checkNotEquals(a, b);
        b.setOsName("a");
        checkEquals(a, b);

        /* OS version. */
        a.setOsVersion("a");
        checkNotEquals(a, b);
        b.setOsVersion("b");
        checkNotEquals(a, b);
        b.setOsVersion("a");
        checkEquals(a, b);

        /* OS build. */
        a.setOsBuild("a");
        checkNotEquals(a, b);
        b.setOsBuild("b");
        checkNotEquals(a, b);
        b.setOsBuild("a");
        checkEquals(a, b);

        /* OS API level. */
        a.setOsApiLevel(1);
        checkNotEquals(a, b);
        b.setOsApiLevel(2);
        checkNotEquals(a, b);
        b.setOsApiLevel(1);
        checkEquals(a, b);

        /* Locale. */
        a.setLocale("a");
        checkNotEquals(a, b);
        b.setLocale("b");
        checkNotEquals(a, b);
        b.setLocale("a");
        checkEquals(a, b);

        /* Time zone offset. */
        a.setTimeZoneOffset(1);
        checkNotEquals(a, b);
        b.setTimeZoneOffset(2);
        checkNotEquals(a, b);
        b.setTimeZoneOffset(1);
        checkEquals(a, b);

        /* Screen size. */
        a.setScreenSize("a");
        checkNotEquals(a, b);
        b.setScreenSize("b");
        checkNotEquals(a, b);
        b.setScreenSize("a");
        checkEquals(a, b);

        /* App version. */
        a.setAppVersion("a");
        checkNotEquals(a, b);
        b.setAppVersion("b");
        checkNotEquals(a, b);
        b.setAppVersion("a");
        checkEquals(a, b);

        /* Carrier name. */
        a.setCarrierName("a");
        checkNotEquals(a, b);
        b.setCarrierName("b");
        checkNotEquals(a, b);
        b.setCarrierName("a");
        checkEquals(a, b);

        /* Carrier country. */
        a.setCarrierCountry("a");
        checkNotEquals(a, b);
        b.setCarrierCountry("b");
        checkNotEquals(a, b);
        b.setCarrierCountry("a");
        checkEquals(a, b);

        /* App build. */
        a.setAppBuild("a");
        checkNotEquals(a, b);
        b.setAppBuild("b");
        checkNotEquals(a, b);
        b.setAppBuild("a");
        checkEquals(a, b);

        /* App namespace. */
        a.setAppNamespace("a");
        checkNotEquals(a, b);
        b.setAppNamespace("b");
        checkNotEquals(a, b);
        b.setAppNamespace("a");
        checkEquals(a, b);
    }
}
