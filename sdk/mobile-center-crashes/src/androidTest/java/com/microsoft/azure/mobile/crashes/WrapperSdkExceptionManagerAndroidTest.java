package com.microsoft.azure.mobile.crashes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static com.microsoft.azure.mobile.test.TestUtils.TAG;
import static org.junit.Assert.assertTrue;

public class WrapperSdkExceptionManagerAndroidTest {

    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        MobileCenterLog.setLogLevel(android.util.Log.VERBOSE);
        sContext = InstrumentationRegistry.getContext();
        Constants.loadFromContext(sContext);
        StorageHelper.initialize(sContext);
    }

    @Before
    public void cleanup() {
        android.util.Log.i(TAG, "Cleanup");
        Crashes.unsetInstance();
        StorageHelper.PreferencesStorage.clear();
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            assertTrue(logFile.delete());
        }
    }

    @Test
    public void managedExceptionData() {

        class ErrorData {
            byte[] data;
            UUID id;
        }

        ErrorData errorA = new ErrorData();
        errorA.data = new byte[]{};
        errorA.id = UUID.randomUUID();

        ErrorData errorB = new ErrorData();
        errorB.data = null;
        errorB.id = UUID.randomUUID();

        ErrorData errorC = new ErrorData();
        errorC.data = new byte[]{'d','a','t','a'};
        errorC.id = UUID.randomUUID();

        ErrorData[] errors = new ErrorData[]{errorA, errorB, errorC};

        for (ErrorData error : errors) {
            WrapperSdkExceptionManager.saveWrapperExceptionData(error.data, error.id.toString());

            byte[] loadedData = WrapperSdkExceptionManager.loadWrapperExceptionData(error.id.toString());

            if (error.data == null) {
                assert(loadedData == null);
                continue;
            }

            for (int i = 0; i < error.data.length; ++i) {
                assert(error.data[i] == loadedData[i]);
            }
        }

        WrapperSdkExceptionManager.saveWrapperExceptionData(null, null);

        WrapperSdkExceptionManager.deleteWrapperExceptionData(null);
        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        WrapperSdkExceptionManager.deleteWrapperExceptionData(errorA.id);

        // even after deleting errorA, it should exist in memory - so, we can still load it
        byte[] loadedDataA = WrapperSdkExceptionManager.loadWrapperExceptionData(errorA.id.toString());
        for (int i = 0; i < errorA.data.length; ++i) {
            assert(errorA.data[i] == loadedDataA[i]);
        }

        WrapperSdkExceptionManager.loadWrapperExceptionData(null);
    }

}
