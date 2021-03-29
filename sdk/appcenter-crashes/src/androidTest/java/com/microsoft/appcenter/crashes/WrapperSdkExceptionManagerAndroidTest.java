/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import android.annotation.SuppressLint;
import android.app.Application;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterPrivateHelper;
import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WrapperSdkExceptionManagerAndroidTest {

    @SuppressLint("StaticFieldLeak")
    private static Application sApplication;

    @BeforeClass
    public static void setUpClass() {
        sApplication = (Application) InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext();
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

    private void startFresh() {

        /* Configure new instance. */
        AppCenterPrivateHelper.unsetInstance();
        Crashes.unsetInstance();
        AppCenter.setLogLevel(android.util.Log.VERBOSE);
        AppCenter.configure(sApplication, "a");

        /* Clean state. */
        AppCenter.setEnabled(false);
        AppCenter.setEnabled(true).get();

        /* Replace channel to avoid trying to send logs. */
        AppCenter.getInstance().setChannel(mock(Channel.class));

        /* Start crashes. */
        AppCenter.start(Crashes.class);

        /* Wait for start. */
        Assert.assertTrue(Crashes.isEnabled().get());
    }

    @Test
    public void saveWrapperException() {

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

    @Test
    public void buildHandledErrorReport() {

        /* If we start the Crashes sdk. */
        long beforeStartTime = System.currentTimeMillis();
        startFresh();
        long afterStartTime = System.currentTimeMillis();

        /* When we build an error report for an handled error. */
        long beforeBuildTime = System.currentTimeMillis();
        String errorReportId = UUID.randomUUID().toString();
        ErrorReport errorReport = WrapperSdkExceptionManager.buildHandledErrorReport(sApplication, errorReportId);
        long afterBuildTime = System.currentTimeMillis();

        /* Then it contains the following properties. */
        assertNotNull(errorReport);
        assertEquals(errorReportId, errorReport.getId());
        assertNotNull(errorReport.getDevice());
        assertNotNull(errorReport.getAppErrorTime());
        assertNotNull(errorReport.getAppStartTime());
        assertNull(errorReport.getStackTrace());
        assertNull(errorReport.getThreadName());

        /* Check start time is consistent. */
        assertTrue(errorReport.getAppStartTime().getTime() >= beforeStartTime);
        assertTrue(errorReport.getAppStartTime().getTime() <= afterStartTime);

        /* Check error time is consistent. */
        assertTrue(errorReport.getAppErrorTime().getTime() >= beforeBuildTime);
        assertTrue(errorReport.getAppErrorTime().getTime() <= afterBuildTime);

        /* Check device info is cached. */
        ErrorReport errorReport2 = WrapperSdkExceptionManager.buildHandledErrorReport(sApplication, UUID.randomUUID().toString());
        assertSame(errorReport.getDevice(), errorReport2.getDevice());
    }
}
