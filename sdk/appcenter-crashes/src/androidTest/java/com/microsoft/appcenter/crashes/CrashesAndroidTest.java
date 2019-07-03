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
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.model.NativeException;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.microsoft.appcenter.Flags.CRITICAL;
import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.test.TestUtils.TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class CrashesAndroidTest {

    @Parameterized.Parameter
    public String mUserId;

    @Parameterized.Parameters(name = "userId={0}")
    public static Collection<String> userIds() {
        return Arrays.asList(null, "alice");
    }

    @SuppressLint("StaticFieldLeak")
    private static Application sApplication;

    private static Thread.UncaughtExceptionHandler sDefaultCrashHandler;

    private Channel mChannel;

    /* Filter out the minidump folder. */
    private final FileFilter mMinidumpFilter = new FileFilter() {

        @Override
        public boolean accept(File file) {
            return !file.isDirectory();
        }
    };

    @BeforeClass
    public static void setUpClass() {
        sDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        sApplication = (Application) InstrumentationRegistry.getContext().getApplicationContext();
        FileManager.initialize(sApplication);
        SharedPreferencesManager.initialize(sApplication);
        Constants.loadFromContext(sApplication);
    }

    @Before
    public void setUp() {
        Thread.setDefaultUncaughtExceptionHandler(sDefaultCrashHandler);
        SharedPreferencesManager.clear();
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            if (logFile.isDirectory()) {
                for (File dumpDir : logFile.listFiles()) {
                    for (File dumpFile : dumpDir.listFiles()) {
                        assertTrue(dumpFile.delete());
                    }
                }
            } else {
                assertTrue(logFile.delete());
            }
        }
        mChannel = mock(Channel.class);
    }

    @After
    public void tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(sDefaultCrashHandler);
    }

    private static Error generateStackOverflowError() {
        try {
            return generateStackOverflowError();
        } catch (StackOverflowError error) {
            return error;
        }
    }

    private void startFresh(CrashesListener listener) {

        /* Configure new instance. */
        AppCenterPrivateHelper.unsetInstance();
        Crashes.unsetInstance();
        AppCenter.setLogLevel(android.util.Log.VERBOSE);
        AppCenter.configure(sApplication, "a");

        /* Clean logs. */
        AppCenter.setEnabled(false);
        AppCenter.setEnabled(true).get();

        /* Replace channel. */
        AppCenter.getInstance().setChannel(mChannel);

        /* Set listener. */
        Crashes.setListener(listener);

        /* Set user identifier. */
        AppCenter.setUserId(mUserId);

        /* Start crashes. */
        AppCenter.start(Crashes.class);

        /* Wait for start. */
        assertTrue(Crashes.isEnabled().get());
    }

    @Test
    public void getLastSessionCrashReportSimpleException() throws Exception {

        /* Null before start. */
        Crashes.unsetInstance();
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());

        /* Crash on 1st process. */
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        startFresh(null);
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());
        final RuntimeException exception = new IllegalArgumentException();
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();

        /* Get last session crash on 2nd process. */
        startFresh(null);
        ErrorReport errorReport = Crashes.getLastSessionCrashReport().get();
        assertNotNull(errorReport);
        Throwable lastThrowable = errorReport.getThrowable();
        assertTrue(lastThrowable instanceof IllegalArgumentException);
        assertTrue(Crashes.hasCrashedInLastSession().get());

        /* Disable SDK, that will clear the report. */
        Crashes.setEnabled(false).get();
        errorReport = Crashes.getLastSessionCrashReport().get();
        assertNull(errorReport);

        /* The report must not be restored after re-enabling. */
        Crashes.setEnabled(true).get();
        errorReport = Crashes.getLastSessionCrashReport().get();
        assertNull(errorReport);
    }

    @Test
    public void getLastSessionCrashReportStackOverflowException() throws Exception {

        /* Null before start. */
        Crashes.unsetInstance();
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());

        /* Crash on 1st process. */
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        startFresh(null);
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());
        final Error exception = generateStackOverflowError();
        assertTrue(exception.getStackTrace().length > ErrorLogHelper.FRAME_LIMIT);
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();

        /* Get last session crash on 2nd process. */
        startFresh(null);
        ErrorReport errorReport = Crashes.getLastSessionCrashReport().get();
        assertNotNull(errorReport);
        Throwable lastThrowable = errorReport.getThrowable();
        assertTrue(lastThrowable instanceof StackOverflowError);
        assertEquals(ErrorLogHelper.FRAME_LIMIT, lastThrowable.getStackTrace().length);
        assertTrue(Crashes.hasCrashedInLastSession().get());
    }

    @Test
    public void getLastSessionCrashReportNative() throws Exception {

        /* Null before start. */
        Crashes.unsetInstance();
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getMinidumpDirectory().get());

        /* Simulate we have a minidump. */
        File newMinidumpDirectory = ErrorLogHelper.getNewMinidumpDirectory();
        File minidumpFile = new File(newMinidumpDirectory, "minidump.dmp");
        FileManager.write(minidumpFile, "mock minidump");

        /* Start crashes now. */
        startFresh(null);

        /* We can access directory now. */
        assertEquals(newMinidumpDirectory.getAbsolutePath(), Crashes.getMinidumpDirectory().get());
        ErrorReport errorReport = Crashes.getLastSessionCrashReport().get();
        assertNotNull(errorReport);
        assertTrue(Crashes.hasCrashedInLastSession().get());
        assertTrue(errorReport.getThrowable() instanceof NativeException);

        /* File has been deleted. */
        assertFalse(minidumpFile.exists());

        /* After restart, it's processed. */
        Crashes.unsetInstance();
        startFresh(null);
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());
    }

    @Test
    public void failedToMoveMinidump() throws Exception {

        /* Simulate we have a minidump. */
        File newMinidumpDirectory = ErrorLogHelper.getNewMinidumpDirectory();
        File minidumpFile = new File(newMinidumpDirectory, "minidump.dmp");
        FileManager.write(minidumpFile, "mock minidump");

        /* Make moving fail. */
        assertTrue(ErrorLogHelper.getPendingMinidumpDirectory().delete());

        /* Start crashes now. */
        try {
            startFresh(null);

            /* If failed to process minidump, delete entire crash. */
            assertNull(Crashes.getLastSessionCrashReport().get());
            assertFalse(Crashes.hasCrashedInLastSession().get());
            assertFalse(minidumpFile.exists());
        } finally {
            assertTrue(ErrorLogHelper.getPendingMinidumpDirectory().mkdir());
        }
    }

    @Test
    public void clearInvalidFiles() throws Exception {
        File invalidFile1 = new File(ErrorLogHelper.getErrorStorageDirectory(), UUID.randomUUID() + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION);
        File invalidFile2 = new File(ErrorLogHelper.getErrorStorageDirectory(), UUID.randomUUID() + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION);
        assertTrue(invalidFile1.createNewFile());
        new FileWriter(invalidFile2).append("fake_data").close();
        assertEquals(2, ErrorLogHelper.getStoredErrorLogFiles().length);

        /* Invalid files should be cleared. */
        startFresh(null);
        assertTrue(Crashes.isEnabled().get());
        assertEquals(0, ErrorLogHelper.getStoredErrorLogFiles().length);
    }

    @Test
    public void testNoDuplicateCallbacksOrSending() throws Exception {

        /* Crash on 1st process. */
        Crashes.unsetInstance();
        assertFalse(Crashes.hasCrashedInLastSession().get());
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        CrashesListener crashesListener = mock(CrashesListener.class);

        /* While testing should process, call methods that require the handler to test we avoid a dead lock and run directly. */
        when(crashesListener.shouldProcess(any(ErrorReport.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) {
                assertNotNull(AppCenter.getInstallId().get());
                return AppCenter.isEnabled().get() && Crashes.isEnabled().get();
            }
        });
        when(crashesListener.shouldAwaitUserConfirmation()).thenReturn(true);
        startFresh(crashesListener);
        final RuntimeException exception = new RuntimeException();
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();
        verify(uncaughtExceptionHandler).uncaughtException(thread, exception);
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);
        verifyZeroInteractions(crashesListener);

        /* Second process: enqueue log but network is down... */
        android.util.Log.i(TAG, "Process 2");
        startFresh(crashesListener);

        /* Check last session error report. */
        Crashes.getLastSessionCrashReport().thenAccept(new AppCenterConsumer<ErrorReport>() {

            @Override
            public void accept(ErrorReport errorReport) {
                assertNotNull(errorReport);
                Throwable lastThrowable = errorReport.getThrowable();
                assertTrue(lastThrowable instanceof RuntimeException);
            }
        });
        assertTrue(Crashes.hasCrashedInLastSession().get());

        /* Wait U.I. thread callback (shouldAwaitUserConfirmation). */
        final Semaphore semaphore = new Semaphore(0);
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                semaphore.release();
            }
        });
        semaphore.acquire();

        /* Waiting user confirmation so no log sent yet. */
        verify(mChannel, never()).enqueue(isA(ManagedErrorLog.class), anyString(), anyInt());
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);
        verify(crashesListener).shouldProcess(any(ErrorReport.class));
        verify(crashesListener).shouldAwaitUserConfirmation();
        verifyNoMoreInteractions(crashesListener);

        /* Confirm to resume processing. */
        Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);
        assertTrue(Crashes.isEnabled().get());
        ArgumentCaptor<ManagedErrorLog> log = ArgumentCaptor.forClass(ManagedErrorLog.class);
        verify(mChannel).enqueue(log.capture(), anyString(), eq(CRITICAL));
        assertNotNull(log.getValue());
        assertEquals(mUserId, log.getValue().getUserId());
        assertEquals(1, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);

        verify(crashesListener).getErrorAttachments(any(ErrorReport.class));
        verifyNoMoreInteractions(crashesListener);

        /* Third process: sending succeeds. */
        android.util.Log.i(TAG, "Process 3");
        mChannel = mock(Channel.class);
        ArgumentCaptor<Channel.GroupListener> groupListener = ArgumentCaptor.forClass(Channel.GroupListener.class);
        startFresh(crashesListener);
        verify(mChannel).addGroup(anyString(), anyInt(), anyInt(), anyInt(), isNull(Ingestion.class), groupListener.capture());
        groupListener.getValue().onBeforeSending(log.getValue());
        groupListener.getValue().onSuccess(log.getValue());

        /* Wait callback to be processed in background thread (file manipulations) then called back in UI. */

        /*
         * Wait background thread to process the 2 previous commands,
         * to do we check if crashed in last session, since we restarted process again after crash,
         * it's false even if we couldn't send the log yet.
         */
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());

        /* Wait U.I. thread callbacks. */
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                semaphore.release();
            }
        });
        semaphore.acquire();

        assertEquals(0, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);
        verify(mChannel, never()).enqueue(isA(ManagedErrorLog.class), anyString(), anyInt());
        verify(crashesListener).onBeforeSending(any(ErrorReport.class));
        verify(crashesListener).onSendingSucceeded(any(ErrorReport.class));
        verifyNoMoreInteractions(crashesListener);
    }

    @Test
    public void processingWithMinidump() throws Exception {

        /* Simulate we have a minidump. */
        File newMinidumpDirectory = ErrorLogHelper.getNewMinidumpDirectory();
        File minidumpFile = new File(newMinidumpDirectory, "minidump.dmp");
        FileManager.write(minidumpFile, "mock minidump");

        /* Set up crash listener. */
        CrashesListener crashesListener = mock(CrashesListener.class);
        when(crashesListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(crashesListener.shouldAwaitUserConfirmation()).thenReturn(true);
        ErrorAttachmentLog textAttachment = ErrorAttachmentLog.attachmentWithText("Hello", "hello.txt");
        when(crashesListener.getErrorAttachments(any(ErrorReport.class))).thenReturn(Collections.singletonList(textAttachment));
        startFresh(crashesListener);

        /* Check last session error report. */
        assertTrue(Crashes.hasCrashedInLastSession().get());

        /* Wait U.I. thread callback (shouldAwaitUserConfirmation). */
        final Semaphore semaphore = new Semaphore(0);
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                semaphore.release();
            }
        });
        semaphore.acquire();

        /* Waiting user confirmation so no log sent yet. */
        verify(mChannel, never()).enqueue(isA(ManagedErrorLog.class), anyString(), anyInt());
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);
        verify(crashesListener).shouldProcess(any(ErrorReport.class));
        verify(crashesListener).shouldAwaitUserConfirmation();
        verifyNoMoreInteractions(crashesListener);

        /* Confirm to resume processing. */
        Crashes.notifyUserConfirmation(Crashes.SEND);
        assertTrue(Crashes.isEnabled().get());
        ArgumentCaptor<ManagedErrorLog> managedErrorLog = ArgumentCaptor.forClass(ManagedErrorLog.class);
        verify(mChannel).enqueue(managedErrorLog.capture(), anyString(), eq(CRITICAL));
        assertNotNull(managedErrorLog.getValue());
        assertEquals(mUserId, managedErrorLog.getValue().getUserId());
        assertNotNull(managedErrorLog.getValue().getException());
        assertNull(managedErrorLog.getValue().getException().getMinidumpFilePath());
        assertEquals(1, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);
        verify(crashesListener).getErrorAttachments(any(ErrorReport.class));
        verifyNoMoreInteractions(crashesListener);

        /* Verify automatic minidump attachment. */
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                if (argument instanceof ErrorAttachmentLog) {
                    ErrorAttachmentLog log = (ErrorAttachmentLog) argument;
                    return "application/octet-stream".equals(log.getContentType()) && "minidump.dmp".equals(log.getFileName());
                }
                return false;
            }
        }), anyString(), eq(DEFAULTS));

        /* Verify custom text attachment. */
        verify(mChannel).enqueue(eq(textAttachment), anyString(), eq(DEFAULTS));
    }

    @Test
    public void cleanupFilesOnDisable() throws Exception {

        /* Crash. */
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        startFresh(null);
        assertTrue(Crashes.isEnabled().get());
        final RuntimeException exception = new RuntimeException();
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();
        verify(uncaughtExceptionHandler).uncaughtException(thread, exception);
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);

        /* Disable, test waiting for disable to finish. */
        Crashes.setEnabled(false).get();
        assertFalse(Crashes.isEnabled().get());
        assertEquals(0, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);
    }

    @Test
    public void setEnabledWhileAlreadyEnabledShouldNotDuplicateCrashReport() throws Exception {

        /* Test the fix of the duplicate crash sending bug. */
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        startFresh(null);
        Crashes.setEnabled(true);
        assertTrue(Crashes.isEnabled().get());
        final RuntimeException exception = new RuntimeException();
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();
        verify(uncaughtExceptionHandler).uncaughtException(thread, exception);

        /* Check there are only 2 files: the throwable and the json one. */
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles(mMinidumpFilter).length);
    }
}
