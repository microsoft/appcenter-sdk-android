/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile;


import com.microsoft.azure.mobile.utils.DeviceInfoHelper;
import com.microsoft.azure.mobile.utils.IdHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Test;

/**
 * This class is to go through the code to get dummy code coverage for constructors.
 */
@SuppressWarnings("unused")
public class InstantiationTest {

    @Test
    public void storageHelper() {
        new StorageHelper();
        new StorageHelper.PreferencesStorage();
        new StorageHelper.InternalStorage();
    }

    @Test
    public void deviceInfoHelper() {
        new DeviceInfoHelper();
    }

    @Test
    public void constants() {
        new Constants();
    }

    @Test
    public void prefStorageConstants() {
        new PrefStorageConstants();
    }

    @Test
    public void idHelper() {
        new IdHelper();
    }

    @Test
    public void mobileCenterLog() {
        new MobileCenterLog();
    }
}
