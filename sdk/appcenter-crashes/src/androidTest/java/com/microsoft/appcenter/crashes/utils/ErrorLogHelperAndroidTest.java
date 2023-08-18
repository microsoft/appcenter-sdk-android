/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.utils.storage.FileManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

@SuppressWarnings("unused")
public class ErrorLogHelperAndroidTest {

    private File mErrorDirectory;

    @BeforeClass
    public static void setUpClass() {
        Constants.loadFromContext(InstrumentationRegistry.getInstrumentation().getContext());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setUp() {
        mErrorDirectory = ErrorLogHelper.getErrorStorageDirectory();
        assertNotNull(mErrorDirectory);
        File[] files = mErrorDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        assertTrue(mErrorDirectory.exists());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        File[] files = mErrorDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        ErrorLogHelper.clearStaticState();
        mErrorDirectory.delete();
        assertFalse(mErrorDirectory.exists());
    }

    @Test
    public void getErrorStorageDirectory() {
        assertEquals(Constants.FILES_PATH, mErrorDirectory.getParent());
        assertEquals(ErrorLogHelper.ERROR_DIRECTORY, mErrorDirectory.getName());
    }

    @Test
    public void removeMinidumpFolder() {
        File minidumpFolder = new File(mErrorDirectory.getAbsolutePath(), "minidump");
        ErrorLogHelper.getNewMinidumpSubfolder();
        assertTrue(minidumpFolder.exists());
        ErrorLogHelper.removeMinidumpFolder();
        assertFalse(minidumpFolder.exists());
    }

    @Test
    public void getStoredFile() throws Exception {
        File[] testFiles = new File[4];

        /* Get all error logs stored in the file system when no logs exist. */
        File[] files = ErrorLogHelper.getStoredErrorLogFiles();
        assertNotNull(files);
        assertEquals(0, files.length);

        /* Generate test files. */
        long date = 1000000;
        for (int i = 0; i < 3; i++) {
            File file = new File(mErrorDirectory, new UUID(0, i) + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION);

            /*
             * file.setLastModified does not work on most devices or emulators and returns false.
             * So we can only sleep at least 1 second between each to make sure the time is different...
             */
            FileManager.write(file, "contents");
            testFiles[i] = file;
            Thread.sleep(1000);
        }

        assertEquals(testFiles[2], ErrorLogHelper.getLastErrorLogFile());

        testFiles[3] = new File(mErrorDirectory, new UUID(0, 3) + ErrorLogHelper.THROWABLE_FILE_EXTENSION);
        FileManager.write(testFiles[3], "contents");

        /* Get all error logs stored in the file system when logs exist. */
        files = ErrorLogHelper.getStoredErrorLogFiles();
        assertNotNull(files);
        assertEquals(3, files.length);

        /* Get an error log file by UUID. */
        File file = ErrorLogHelper.getStoredErrorLogFile(new UUID(0, 0));
        assertNotNull(file);
        assertEquals(testFiles[0], file);
        file = ErrorLogHelper.getStoredErrorLogFile(new UUID(0, 3));
        assertNull(file);

        /* Remove an error log file. */
        ErrorLogHelper.removeStoredErrorLogFile(new UUID(0, 2));
        file = ErrorLogHelper.getStoredErrorLogFile(new UUID(0, 2));
        assertNull(file);

        /* Verify the number of remaining error log files. */
        files = ErrorLogHelper.getStoredErrorLogFiles();
        assertNotNull(files);
        assertEquals(2, files.length);

        /*Trying to delete a throwable file which doesn't exist. */
        ErrorLogHelper.removeStoredThrowableFile(new UUID(0, 1));
        file = ErrorLogHelper.getStoredErrorLogFile(new UUID(0, 1));
        assertNotNull(file);

        /* Clean up. */
        for (int i = 0; i < 2; i++)
            FileManager.delete(testFiles[i]);
    }

    @Test
    public void parseDevice() {
        String contextInfoString = "{\"DEVICE_INFO\":{\"sdkName\":\"appcenter.android\",\"sdkVersion\":\"2.5.4.2\",\"model\":\"Android SDK built for x86\",\"oemName\":\"Google\",\"osName\":\"Android\",\"osVersion\":\"9\",\"osBuild\":\"PSR1.180720.075\",\"osApiLevel\":28,\"locale\":\"en_US\",\"timeZoneOffset\":240,\"screenSize\":\"1080x1794\",\"appVersion\":\"2.5.4.2\",\"carrierName\":\"Android\",\"carrierCountry\":\"us\",\"appBuild\":\"59\",\"appNamespace\":\"com.microsoft.appcenter.sasquatch.project\"}, \"USER_ID\":\"qwerty12345\"}";
        Device device = ErrorLogHelper.parseDevice(contextInfoString);
        String userId = ErrorLogHelper.parseUserId(contextInfoString);
        assertNotNull(device);
        assertNotNull(userId);
        assertEquals(device.getAppBuild(), "59");
        assertEquals(device.getAppVersion(), "2.5.4.2");
        assertEquals(device.getSdkName(), "appcenter.android");

        /* Test empty string. */
        String contextInfo2 = "";
        Device device2 = ErrorLogHelper.parseDevice(contextInfo2);
        String userId2 = ErrorLogHelper.parseUserId(contextInfo2);
        assertNull(device2);
        assertNull(userId2);

        /* Test malformed string. */
        String contextInfo3 = "abcd";
        Device device3 = ErrorLogHelper.parseDevice(contextInfo3);
        String userId3 = ErrorLogHelper.parseUserId(contextInfo3);
        assertNull(device3);
        assertNull(userId3);

        /* Test parse old device data. */
        String contextInfo4 = "{\"sdkName\":\"appcenter.android\",\"sdkVersion\":\"2.5.4.2\",\"model\":\"Android SDK built for x86\",\"oemName\":\"Google\",\"osName\":\"Android\",\"osVersion\":\"9\",\"osBuild\":\"PSR1.180720.075\",\"osApiLevel\":28,\"locale\":\"en_US\",\"timeZoneOffset\":240,\"screenSize\":\"1080x1794\",\"appVersion\":\"2.5.4.2\",\"carrierName\":\"Android\",\"carrierCountry\":\"us\",\"appBuild\":\"59\",\"appNamespace\":\"com.microsoft.appcenter.sasquatch.project\"}";
        Device device4 = ErrorLogHelper.parseDevice(contextInfo4);
        String userId4 = ErrorLogHelper.parseUserId(contextInfo4);
        assertNotNull(device4);
        assertNull(userId4);
    }

    @Test
    public void parseDataResidencyRegion() {
        String mockDataResidencyRegion = "mockRegion";
        String mockContextInformation = "{\"dataResidencyRegion\":\"" + mockDataResidencyRegion + "\"}";
        String result = ErrorLogHelper.parseDataResidencyRegion(mockContextInformation);
        assertEquals(mockDataResidencyRegion, result);
    }

    @Test()
    public void parseDataResidencyRegionIncorrectJson() {
        String invalidJson = "invalidJson";
        String result = ErrorLogHelper.parseDataResidencyRegion(invalidJson);
        assertNull(result);
    }
}
