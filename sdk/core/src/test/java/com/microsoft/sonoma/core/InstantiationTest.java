package com.microsoft.sonoma.core;


import com.microsoft.sonoma.core.utils.DeviceInfoHelper;
import com.microsoft.sonoma.core.utils.StorageHelper;

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
}
