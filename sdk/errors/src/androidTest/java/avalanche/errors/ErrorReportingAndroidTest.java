package avalanche.errors;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import avalanche.core.Constants;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.Log;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.StorageHelper;
import avalanche.errors.model.ErrorReport;
import avalanche.errors.utils.ErrorLogHelper;

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
public class ErrorReportingAndroidTest {

    private static Context sContext;

    private static Thread.UncaughtExceptionHandler sDefaultCrashHandler;

    @BeforeClass
    public static void setUpClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);
        sContext = InstrumentationRegistry.getContext();
        Constants.loadFromContext(sContext);
        StorageHelper.initialize(sContext);
        sDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @After
    @Before
    public void cleanup() {
        AvalancheLog.info("Cleanup test");
        Thread.setDefaultUncaughtExceptionHandler(sDefaultCrashHandler);
        StorageHelper.PreferencesStorage.remove(ErrorReporting.PREF_KEY_ALWAYS_SEND);
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            assertTrue(logFile.delete());
        }
    }

    @Test
    public void testNoDupicateCallbacksOrSending() throws InterruptedException {

        /* Crash on 1st process. */
        AvalancheLog.info("Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        AvalancheChannel channel = mock(AvalancheChannel.class);
        ErrorReporting.getInstance().onChannelReady(sContext, channel);
        ErrorReportingListener errorReportingListener = mock(ErrorReportingListener.class);
        when(errorReportingListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(errorReportingListener.shouldAwaitUserConfirmation()).thenReturn(true);
        ErrorReporting.setListener(errorReportingListener);
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
        verifyZeroInteractions(errorReportingListener);

        /* Second process: enqueue log but network is down... */
        AvalancheLog.info("Process 2");
        final AtomicReference<Log> log = new AtomicReference<>();
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.set((Log) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(channel).enqueue(any(Log.class), anyString());
        ErrorReporting.unsetInstance();
        ErrorReporting.setListener(errorReportingListener);
        ErrorReporting.getInstance().onChannelReady(sContext, channel);

        /* Waiting user confirmation so no log sent yet. */
        verify(channel, never()).enqueue(any(Log.class), anyString());
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(errorReportingListener).shouldProcess(any(ErrorReport.class));
        verify(errorReportingListener).shouldAwaitUserConfirmation();
        verifyNoMoreInteractions(errorReportingListener);

        /* Confirm to resume processing. */
        ErrorReporting.notifyUserConfirmation(ErrorReporting.ALWAYS_SEND);
        verify(channel).enqueue(any(Log.class), anyString());
        assertNotNull(log.get());
        assertEquals(1, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(errorReportingListener).getErrorAttachment(any(ErrorReport.class));
        verifyNoMoreInteractions(errorReportingListener);

        /* Third process: sending succeeds. */
        AvalancheLog.info("Process 3");
        final AtomicReference<AvalancheChannel.GroupListener> groupListener = new AtomicReference<>();
        channel = mock(AvalancheChannel.class);
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                AvalancheChannel.GroupListener listener = (AvalancheChannel.GroupListener) invocationOnMock.getArguments()[4];
                groupListener.set(listener);
                listener.onBeforeSending(log.get());
                return null;
            }
        }).when(channel).addGroup(anyString(), anyInt(), anyInt(), anyInt(), any(AvalancheChannel.GroupListener.class));
        ErrorReporting.unsetInstance();
        ErrorReporting.setListener(errorReportingListener);
        ErrorReporting.getInstance().onChannelReady(sContext, channel);
        assertNotNull(groupListener.get());
        groupListener.get().onSuccess(log.get());
        assertEquals(0, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        verify(errorReportingListener).onBeforeSending(any(ErrorReport.class));
        verify(errorReportingListener).onSendingSucceeded(any(ErrorReport.class));
        verifyNoMoreInteractions(errorReportingListener);
    }
}
