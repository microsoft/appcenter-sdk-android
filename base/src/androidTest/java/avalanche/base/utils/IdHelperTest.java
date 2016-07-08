package avalanche.base.utils;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;


public class IdHelperTest {

    private static final String TAG = "IdHelper";
    /**
     * Context instance.
     */
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);
    }


    @Test
    public void getInstallId() {
        Log.i(TAG, "Testing installId-shortcut");
        UUID expected = UUID.randomUUID();
        StorageHelper.PreferencesStorage.putString(PrefStorageConstants.KEY_INSTALL_ID, expected.toString());

        UUID actual = IdHelper.getInstallId();
        assertEquals(expected, actual);

        String wrongUUID = "1234567";
        StorageHelper.PreferencesStorage.putString(PrefStorageConstants.KEY_INSTALL_ID, expected.toString());

        actual = IdHelper.getInstallId();
        assertNotEquals(wrongUUID, actual);
        assertNotNull(actual);
    }
}