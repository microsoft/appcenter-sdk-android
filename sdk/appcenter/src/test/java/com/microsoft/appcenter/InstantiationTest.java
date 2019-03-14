/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;


import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

/**
 * This class is to go through the code to get code coverage for constructors of utils/static classes.
 */
@SuppressWarnings("unused")
public class InstantiationTest {

    @Test
    public void fileManager() {
        new FileManager();
    }

    @Test
    public void sharedPreferencesManager() {
        new SharedPreferencesManager();
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
    public void serviceInstrumentationUtils() {
        new ServiceInstrumentationUtils();
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
    public void appCenterLog() {
        new AppCenterLog();
    }

    @Test
    public void flags() {
        new Flags();
    }
}
