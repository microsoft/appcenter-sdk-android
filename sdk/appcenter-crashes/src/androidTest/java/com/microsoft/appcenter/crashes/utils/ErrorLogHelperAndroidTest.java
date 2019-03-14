/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.utils;

import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.utils.storage.FileManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unused")
public class ErrorLogHelperAndroidTest {

    private File mErrorDirectory;

    @BeforeClass
    public static void setUpClass() {
        Constants.loadFromContext(InstrumentationRegistry.getContext());
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
        mErrorDirectory.delete();
    }

    @Test
    public void getErrorStorageDirectory() {
        assertEquals(Constants.FILES_PATH, mErrorDirectory.getParent());
        assertEquals(ErrorLogHelper.ERROR_DIRECTORY, mErrorDirectory.getName());
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
            File file = new File(mErrorDirectory, new UUID(0, i).toString() + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION);

            /*
             * file.setLastModified does not work on most devices or emulators and returns false.
             * So we can only sleep at least 1 second between each to make sure the time is different...
             */
            FileManager.write(file, "contents");
            testFiles[i] = file;
            Thread.sleep(1000);
        }

        assertEquals(testFiles[2], ErrorLogHelper.getLastErrorLogFile());

        testFiles[3] = new File(mErrorDirectory, new UUID(0, 3).toString() + ErrorLogHelper.THROWABLE_FILE_EXTENSION);
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

        /* Get a throwable file by UUID. */
        file = ErrorLogHelper.getStoredThrowableFile(new UUID(0, 3));
        assertNotNull(file);
        assertEquals(testFiles[3], file);
        file = ErrorLogHelper.getStoredThrowableFile(new UUID(0, 0));
        assertNull(file);

        /* Remove an error log file. */
        ErrorLogHelper.removeStoredErrorLogFile(new UUID(0, 2));
        file = ErrorLogHelper.getStoredErrorLogFile(new UUID(0, 2));
        assertNull(file);

        /*Trying to delete an error log file which doesn't exist. */
        ErrorLogHelper.removeStoredErrorLogFile(new UUID(0, 3));
        file = ErrorLogHelper.getStoredThrowableFile(new UUID(0, 3));
        assertNotNull(file);

        /* Verify the number of remaining error log files. */
        files = ErrorLogHelper.getStoredErrorLogFiles();
        assertNotNull(files);
        assertEquals(2, files.length);

        /* Remove a throwable file. */
        ErrorLogHelper.removeStoredThrowableFile(new UUID(0, 3));
        file = ErrorLogHelper.getStoredThrowableFile(new UUID(0, 3));
        assertNull(file);

        /*Trying to delete a throwable file which doesn't exist. */
        ErrorLogHelper.removeStoredThrowableFile(new UUID(0, 1));
        file = ErrorLogHelper.getStoredErrorLogFile(new UUID(0, 1));
        assertNotNull(file);

        /* Clean up. */
        for (int i = 0; i < 2; i++)
            FileManager.delete(testFiles[i]);
    }
}
