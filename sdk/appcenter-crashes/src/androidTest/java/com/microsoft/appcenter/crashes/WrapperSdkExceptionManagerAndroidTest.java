package com.microsoft.appcenter.crashes;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterPrivateHelper;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.TAG;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WrapperSdkExceptionManagerAndroidTest {

    @SuppressLint("StaticFieldLeak")
    private static Application sApplication;

    @BeforeClass
    public static void setUpClass() {
        sApplication = (Application) InstrumentationRegistry.getContext().getApplicationContext();
        StorageHelper.initialize(sApplication);
        Constants.loadFromContext(sApplication);
    }

    @Before
    public void setUp() throws java.lang.Exception {
        android.util.Log.i(TAG, "Cleanup");
        StorageHelper.PreferencesStorage.clear();
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            if (!logFile.isDirectory()) {
                assertTrue(logFile.delete());
            }
        }
    }

    private void startFresh() throws java.lang.Exception {

        /* Configure new instance. */
        AppCenterPrivateHelper.unsetInstance();
        Crashes.unsetInstance();
        AppCenter.setLogLevel(android.util.Log.VERBOSE);
        AppCenter.configure(sApplication, "a");

        /* Replace channel. */
        Method method = AppCenter.class.getDeclaredMethod("getInstance");
        method.setAccessible(true);
        AppCenter appCenter = (AppCenter) method.invoke(null);
        method = AppCenter.class.getDeclaredMethod("setChannel", Channel.class);
        method.setAccessible(true);
        method.invoke(appCenter, mock(Channel.class));

        /* Start crashes. */
        AppCenter.start(Crashes.class);

        /* Wait for start. */
        Assert.assertTrue(Crashes.isEnabled().get());
    }

    @Test
    public void saveWrapperException() throws java.lang.Exception {

        class ErrorData {
            private byte[] data;
            private UUID id;
        }

        ErrorData errorA = new ErrorData();
        errorA.data = new byte[]{};

        ErrorData errorB = new ErrorData();
        errorB.data = null;

        ErrorData errorC = new ErrorData();
        errorC.data = new byte[]{'d', 'a', 't', 'a'};

        ErrorData[] errors = new ErrorData[]{errorA, errorB, errorC};

        for (ErrorData error : errors) {

            /* Reset crash state as only 1 crash is saved per process life time. */
            startFresh();

            /* Save crash. */
            error.id = WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), error.data);
            byte[] loadedData = WrapperSdkExceptionManager.loadWrapperExceptionData(error.id);

            if (error.data == null) {
                assertNull(loadedData);
                continue;
            }

            assertNotNull(loadedData);
            for (int i = 0; i < error.data.length; ++i) {
                assertEquals(error.data[i], loadedData[i]);
            }
        }

        /* Even after deleting errorA, it should exist in memory - so, we can still load it. */
        WrapperSdkExceptionManager.deleteWrapperExceptionData(errorA.id);
        byte[] loadedDataA = WrapperSdkExceptionManager.loadWrapperExceptionData(errorA.id);
        assertNotNull(loadedDataA);
        for (int i = 0; i < errorA.data.length; ++i) {
            assertEquals(errorA.data[i], loadedDataA[i]);
        }

        /* Try to load data bypassing the cache. */
        WrapperSdkExceptionManager.sWrapperExceptionDataContainer.clear();
        byte[] loadedDataC = WrapperSdkExceptionManager.loadWrapperExceptionData(errorC.id);
        assertNotNull(loadedDataC);
        for (int i = 0; i < errorC.data.length; ++i) {
            assertEquals(errorC.data[i], loadedDataC[i]);
        }

        /* Save another crash without reset: will be ignored as only 1 crash per process. */
        assertNull(WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), new byte[]{'e'}));
    }
}
