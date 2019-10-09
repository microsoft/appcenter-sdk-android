/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterPrivateHelper;
import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WrapperSdkExceptionManagerAndroidTest {

    @SuppressLint("StaticFieldLeak")
    private static Application sApplication;

    @BeforeClass
    public static void setUpClass() {
        sApplication = (Application) InstrumentationRegistry.getContext().getApplicationContext();
        SharedPreferencesManager.initialize(sApplication);
        Constants.loadFromContext(sApplication);
    }

    @Before
    public void setUp() {
        android.util.Log.i(TAG, "Cleanup");
        SharedPreferencesManager.clear();
        File[] files = ErrorLogHelper.getErrorStorageDirectory().listFiles();
        assertNotNull(files);
        for (File logFile : files) {
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


        /* Since this is a real Android test, it might actually tries to send crash and might delete files on sending completion. Avoid that. */
        Crashes.setListener(new AbstractCrashesListener() {

            @Override
            public boolean shouldAwaitUserConfirmation() {
                return false;
            }
        });

        /* Start crashes. */
        AppCenter.start(Crashes.class);

        /* Wait for start. */
        Assert.assertTrue(Crashes.isEnabled().get());
    }

    @Test
    public void saveWrapperException() throws java.lang.Exception {

        class ErrorData {
            private String data;
            private UUID id;
        }

        ErrorData errorA = new ErrorData();
        errorA.data = "";

        ErrorData errorB = new ErrorData();
        errorB.data = null;

        ErrorData errorC = new ErrorData();
        errorC.data = "data";

        ErrorData errorD = new ErrorData();
        errorD.data = "otherData";

        ErrorData[] errors = new ErrorData[]{errorA, errorB, errorC, errorD};

        for (ErrorData error : errors) {

            /* Reset crash state as only 1 crash is saved per process life time. */
            startFresh();

            /* Save crash. */
            error.id = WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), error.data);
            String loadedData = WrapperSdkExceptionManager.loadWrapperExceptionData(error.id);

            if (error.data == null) {
                assertNull(loadedData);
                continue;
            }

            assertNotNull(loadedData);
            assertEquals(error.data, loadedData);
        }

        /* Even after deleting errorA and errorD, they should exist in memory - so, we can still load it. */
        WrapperSdkExceptionManager.deleteWrapperExceptionData(errorA.id);
        String loadedDataA = WrapperSdkExceptionManager.loadWrapperExceptionData(errorA.id);
        assertNotNull(loadedDataA);
        assertEquals(errorA.data, loadedDataA);
        WrapperSdkExceptionManager.deleteWrapperExceptionData(errorD.id);
        String loadedDataD = WrapperSdkExceptionManager.loadWrapperExceptionData(errorD.id);
        assertNotNull(loadedDataD);
        assertEquals(errorD.data, loadedDataD);

        /* Try to load data bypassing the cache. */
        WrapperSdkExceptionManager.sWrapperExceptionDataContainer.clear();
        String loadedDataC = WrapperSdkExceptionManager.loadWrapperExceptionData(errorC.id);
        assertNotNull(loadedDataC);
        assertEquals(errorC.data, loadedDataC);

        /* Save another crash without reset: will be ignored as only 1 crash per process. */
        assertNull(WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), "e"));
    }
}
