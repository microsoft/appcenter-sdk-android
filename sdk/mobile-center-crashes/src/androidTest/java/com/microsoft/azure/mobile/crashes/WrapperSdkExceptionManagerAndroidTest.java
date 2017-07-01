package com.microsoft.azure.mobile.crashes;

import android.app.Application;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.crashes.ingestion.models.Exception;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static com.microsoft.azure.mobile.test.TestUtils.TAG;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WrapperSdkExceptionManagerAndroidTest {

    @BeforeClass
    public static void setUpClass() {
        MobileCenterLog.setLogLevel(android.util.Log.VERBOSE);
        Context context = InstrumentationRegistry.getContext();
        MobileCenter.configure((Application) context.getApplicationContext(), "dummy");
    }

    @Before
    public void cleanup() {
        android.util.Log.i(TAG, "Cleanup");
        StorageHelper.PreferencesStorage.clear();
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            assertTrue(logFile.delete());
        }
    }

    @Test
    public void saveWrapperException() {

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
            Crashes.unsetInstance();
            Crashes.getInstance().onStarted(InstrumentationRegistry.getContext(), "", mock(Channel.class));

            /* Save crash. */
            error.id = WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), new Exception(), error.data);
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
        assertNull(WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), new Exception(), new byte[]{'e'}));
    }
}
