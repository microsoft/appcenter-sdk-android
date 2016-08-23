package avalanche.core;


import org.junit.Test;

import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.StorageHelper;

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
