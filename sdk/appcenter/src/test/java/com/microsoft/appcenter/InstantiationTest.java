package com.microsoft.appcenter;


import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.Test;

/**
 * This class is to go through the code to get code coverage for constructors of utils/static classes.
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
}
