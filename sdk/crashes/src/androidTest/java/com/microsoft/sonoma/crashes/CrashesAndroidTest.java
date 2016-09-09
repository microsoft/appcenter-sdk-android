package com.microsoft.sonoma.crashes;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.sonoma.core.Constants;
import com.microsoft.sonoma.core.channel.Channel;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.crashes.model.ErrorReport;
import com.microsoft.sonoma.crashes.utils.ErrorLogHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.sonoma.test.TestUtils.TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class CrashesAndroidTest {

    private static Context sContext;

    private static Thread.UncaughtExceptionHandler sDefaultCrashHandler;

    @BeforeClass
    public static void setUpClass() {
        SonomaLog.setLogLevel(android.util.Log.VERBOSE);
        sContext = InstrumentationRegistry.getContext();
        Constants.loadFromContext(sContext);
        StorageHelper.initialize(sContext);
        sDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Before
    public void cleanup() {
        android.util.Log.i(TAG, "Cleanup");
        Thread.setDefaultUncaughtExceptionHandler(sDefaultCrashHandler);
        StorageHelper.PreferencesStorage.clear();
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            assertTrue(logFile.delete());
        }
    }

    @Test
    public void testNoDuplicateCallbacksOrSending() throws InterruptedException {

        /* Crash on 1st process. */
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        Channel channel = mock(Channel.class);
        Crashes.getInstance().onChannelReady(sContext, channel);
        CrashesListener crashesListener = mock(CrashesListener.class);
        when(crashesListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(crashesListener.shouldAwaitUserConfirmation()).thenReturn(true);
        Crashes.setListener(crashesListener);
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
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verifyZeroInteractions(crashesListener);

        /* Second process: enqueue log but network is down... */
        android.util.Log.i(TAG, "Process 2");
        final AtomicReference<Log> log = new AtomicReference<>();
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.set((Log) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(channel).enqueue(any(Log.class), anyString());
        Crashes.unsetInstance();
        Crashes.setListener(crashesListener);
        Crashes.getInstance().onChannelReady(sContext, channel);

        /* Waiting user confirmation so no log sent yet. */
        verify(channel, never()).enqueue(any(Log.class), anyString());
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(crashesListener).shouldProcess(any(ErrorReport.class));
        verify(crashesListener).shouldAwaitUserConfirmation();
        verifyNoMoreInteractions(crashesListener);

        /* Confirm to resume processing. */
        Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);
        verify(channel).enqueue(any(Log.class), anyString());
        assertNotNull(log.get());
        assertEquals(1, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(crashesListener).getErrorAttachment(any(ErrorReport.class));
        verifyNoMoreInteractions(crashesListener);

        /* Third process: sending succeeds. */
        android.util.Log.i(TAG, "Process 3");
        final AtomicReference<Channel.GroupListener> groupListener = new AtomicReference<>();
        channel = mock(Channel.class);
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Channel.GroupListener listener = (Channel.GroupListener) invocationOnMock.getArguments()[4];
                groupListener.set(listener);
                listener.onBeforeSending(log.get());
                return null;
            }
        }).when(channel).addGroup(anyString(), anyInt(), anyInt(), anyInt(), any(Channel.GroupListener.class));
        Crashes.unsetInstance();
        Crashes.setListener(crashesListener);
        Crashes.getInstance().onChannelReady(sContext, channel);
        assertNotNull(groupListener.get());
        groupListener.get().onSuccess(log.get());
        assertEquals(0, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        verify(crashesListener).onBeforeSending(any(ErrorReport.class));
        verify(crashesListener).onSendingSucceeded(any(ErrorReport.class));
        verifyNoMoreInteractions(crashesListener);
    }

    @Test
    public void cleanupFilesOnDisable() throws InterruptedException {

        /* Crash on 1st process. */
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        Crashes.getInstance().onChannelReady(sContext, mock(Channel.class));
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
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);

        /* Disable in process 2. */
        Crashes.unsetInstance();
        Crashes.setEnabled(false);
        assertEquals(0, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
    }
}
