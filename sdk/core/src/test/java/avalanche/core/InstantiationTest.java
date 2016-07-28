package avalanche.core;


import org.junit.Test;

import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.StorageHelper;

/**
 * This class is to go through the code to get code coverage for constructors.
 * TODO: Temporary and need to be removed.
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
}
