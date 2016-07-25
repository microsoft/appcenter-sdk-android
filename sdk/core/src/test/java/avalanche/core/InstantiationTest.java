package avalanche.core;


import org.junit.Test;

import java.io.IOException;

import avalanche.core.ingestion.models.utils.LogUtils;
import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.StorageHelper;

/**
 * This class is to go through the code to get code coverage for constructors.
 * TODO: Temporary and need to be removed.
 */
public class InstantiationTest {
    @Test
    public void storageHelper() throws IOException {
        new StorageHelper();
        new StorageHelper.PreferencesStorage();
        new StorageHelper.InternalStorage();
    }

    @Test
    public void deviceInfoHelper() {
        new DeviceInfoHelper();
    }

    @Test
    public void logUtils() {
        new LogUtils();
    }
}
