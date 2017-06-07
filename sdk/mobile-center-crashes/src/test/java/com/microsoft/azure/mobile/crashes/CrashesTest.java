package com.microsoft.azure.mobile.crashes;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.MobileCenterHandler;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.azure.mobile.crashes.ingestion.models.StackFrame;
import com.microsoft.azure.mobile.crashes.ingestion.models.json.ErrorAttachmentLogFactory;
import com.microsoft.azure.mobile.crashes.ingestion.models.json.ManagedErrorLogFactory;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.crashes.model.TestCrashException;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.ingestion.models.Device;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.async.SimpleConsumer;
import com.microsoft.azure.mobile.utils.async.SimpleFuture;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@PrepareForTest({ErrorLogHelper.class, SystemClock.class, StorageHelper.InternalStorage.class, StorageHelper.PreferencesStorage.class, MobileCenterLog.class, MobileCenter.class, Crashes.class, HandlerUtils.class, Looper.class})
public class CrashesTest {

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final Exception EXCEPTION = new Exception("This is a test exception.");

    private static final String CRASHES_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Crashes.getInstance().getServiceName();

    @Rule
    public final TemporaryFolder errorStorageDirectory = new TemporaryFolder();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private ManagedErrorLog mErrorLog;

    @Mock
    private MobileCenterHandler mMobileCenterHandler;

    private static void assertErrorEquals(ManagedErrorLog errorLog, ErrorReport report) {
        assertNotNull(report);
        assertEquals(errorLog.getId().toString(), report.getId());
        assertEquals(errorLog.getErrorThreadName(), report.getThreadName());
        assertEquals(CrashesTest.EXCEPTION, report.getThrowable());
        assertEquals(errorLog.getToffset() - errorLog.getAppLaunchTOffset(), report.getAppStartTime().getTime());
        assertEquals(errorLog.getToffset(), report.getAppErrorTime().getTime());
        assertEquals(errorLog.getDevice(), report.getDevice());
    }

    @Before
    public void setUp() throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(null);
        Crashes.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(StorageHelper.InternalStorage.class);
        mockStatic(StorageHelper.PreferencesStorage.class);
        mockStatic(MobileCenterLog.class);
        when(SystemClock.elapsedRealtime()).thenReturn(System.currentTimeMillis());

        mockStatic(MobileCenter.class);

        @SuppressWarnings("unchecked")
        SimpleFuture<Boolean> future = (SimpleFuture<Boolean>) mock(SimpleFuture.class);
        when(MobileCenter.isEnabled()).thenReturn(future);
        when(future.get()).thenReturn(true);

        when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(CRASHES_ENABLED_KEY), anyBoolean());

        /* Mock handlers. */
        mockStatic(HandlerUtils.class);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        doAnswer(runNow).when(mMobileCenterHandler).post(any(Runnable.class), any(Runnable.class));

        mErrorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0, true);
    }

    @Test
    public void singleton() {
        Assert.assertSame(Crashes.getInstance(), Crashes.getInstance());
    }

    @Test
    public void initializeWhenDisabled() {

        /* Setup mock. */
        Crashes crashes = Crashes.getInstance();
        mockStatic(ErrorLogHelper.class);
        File dir = mock(File.class);
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        UncaughtExceptionHandler mockHandler = mock(UncaughtExceptionHandler.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(dir);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(dir.listFiles()).thenReturn(new File[]{file1, file2});
        crashes.setUncaughtExceptionHandler(mockHandler);
        when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(false);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));

        /* Test. */
        assertFalse(Crashes.isEnabled().get());
        assertEquals(crashes.getInitializeTimestamp(), -1);
        assertNull(crashes.getUncaughtExceptionHandler());
        verify(mockHandler).unregister();
    }

    @Test
    public void notInit() {

        /* Just check log is discarded without throwing any exception. */
        Crashes.notifyUserConfirmation(Crashes.SEND);
        Crashes.trackException(EXCEPTION);
        verifyStatic(times(2));
        MobileCenterLog.error(eq(MobileCenter.LOG_TAG), anyString());
    }

    @Test
    public void checkConfig() {
        Crashes instance = Crashes.getInstance();
        Map<String, LogFactory> factories = instance.getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(ManagedErrorLog.TYPE) instanceof ManagedErrorLogFactory);
        assertTrue(factories.remove(ErrorAttachmentLog.TYPE) instanceof ErrorAttachmentLogFactory);
        assertTrue(factories.isEmpty());
        assertEquals(1, instance.getTriggerCount());
        assertEquals(Crashes.ERROR_GROUP, instance.getGroupName());
    }

    @Test
    public void setEnabled() {

        /* Setup mock. */
        Crashes crashes = Crashes.getInstance();
        mockStatic(ErrorLogHelper.class);
        Channel mockChannel = mock(Channel.class);
        File dir = mock(File.class);
        File file1 = mock(File.class);
        File file2 = mock(File.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(dir);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(dir.listFiles()).thenReturn(new File[]{file1, file2});

        /* Before start it's disabled. */
        assertFalse(Crashes.isEnabled().get());
        assertEquals(0, crashes.getInitializeTimestamp());

        /* Start. */
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mockChannel);
        verify(mockChannel).removeGroup(eq(crashes.getGroupName()));
        verify(mockChannel).addGroup(eq(crashes.getGroupName()), anyInt(), anyInt(), anyInt(), any(Channel.GroupListener.class));

        /* Test. */
        assertTrue(Crashes.isEnabled().get());
        Crashes.setEnabled(true);
        assertTrue(Crashes.isEnabled().get());
        assertTrue(crashes.getInitializeTimestamp() > 0);
        Crashes.setEnabled(false);
        assertFalse(Crashes.isEnabled().get());
        verify(mockChannel).clear(crashes.getGroupName());
        verify(mockChannel, times(2)).removeGroup(eq(crashes.getGroupName()));
        assertEquals(crashes.getInitializeTimestamp(), -1);
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
        assertFalse(verify(file1).delete());
        assertFalse(verify(file2).delete());
        Crashes.trackException(EXCEPTION);
        verifyNoMoreInteractions(mockChannel);

        /* Enable back, testing double calls. */
        Crashes.setEnabled(true);
        assertTrue(Crashes.isEnabled().get());
        assertTrue(crashes.getInitializeTimestamp() > 0);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
        Crashes.setEnabled(true);
        assertTrue(Crashes.isEnabled().get());
        verify(mockChannel, times(2)).addGroup(eq(crashes.getGroupName()), anyInt(), anyInt(), anyInt(), any(Channel.GroupListener.class));
        Crashes.trackException(EXCEPTION);
        verify(mockChannel, times(1)).enqueue(any(ManagedErrorLog.class), eq(crashes.getGroupName()));
    }

    @Test
    public void setEnabledWithoutContext() {
        Crashes crashes = Crashes.getInstance();
        crashes.setUncaughtExceptionHandler(null);
        crashes.setInstanceEnabled(true);
        assertNull(crashes.getUncaughtExceptionHandler());

        UncaughtExceptionHandler mockHandler = mock(UncaughtExceptionHandler.class);
        crashes.setUncaughtExceptionHandler(mockHandler);
        crashes.setInstanceEnabled(true);
        assertEquals(mockHandler, crashes.getUncaughtExceptionHandler());

        verifyNoMoreInteractions(mockHandler);
    }

    @Test
    public void queuePendingCrashesShouldProcess() throws IOException, ClassNotFoundException, JSONException {

        /* Setup mock. */
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        ErrorReport report = new ErrorReport();
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(report);
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(new RuntimeException());
        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(report)).thenReturn(true);
        when(mockListener.shouldAwaitUserConfirmation()).thenReturn(false);
        ErrorAttachmentLog mockAttachment = mock(ErrorAttachmentLog.class);
        when(mockAttachment.getId()).thenReturn(UUID.randomUUID());
        when(mockAttachment.getErrorId()).thenReturn(UUID.randomUUID());
        when(mockAttachment.getContentType()).thenReturn("");
        when(mockAttachment.getFileName()).thenReturn("");
        when(mockAttachment.getData()).thenReturn(new byte[0]);
        when(mockAttachment.isValid()).thenReturn(true);
        ErrorAttachmentLog mockEmptyAttachment = mock(ErrorAttachmentLog.class);
        final int skipAttachmentLogsCount = 2;
        List<ErrorAttachmentLog> errorAttachmentLogList = Arrays.asList(mockAttachment, mockAttachment, mockEmptyAttachment, null);
        when(mockListener.getErrorAttachments(report)).thenReturn(errorAttachmentLogList);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mErrorLog);
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mockContext, "", mockChannel);

        /* Test. */
        verify(mockListener).shouldProcess(report);
        verify(mockListener).shouldAwaitUserConfirmation();
        verify(mockListener).getErrorAttachments(report);
        verify(mockChannel).enqueue(argThat(new ArgumentMatcher<Log>() {
            @Override
            public boolean matches(Object log) {
                return log.equals(mErrorLog);
            }
        }), eq(crashes.getGroupName()));
        verify(mockChannel, times(errorAttachmentLogList.size() - skipAttachmentLogsCount)).enqueue(mockAttachment, crashes.getGroupName());
    }

    @Test
    public void queuePendingCrashesShouldNotProcess() throws IOException, ClassNotFoundException, JSONException {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);

        ErrorReport report = new ErrorReport();

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(report);
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(new RuntimeException()).thenReturn(new byte[]{});

        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(report)).thenReturn(false);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mErrorLog);

        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mockContext, "", mockChannel);

        verify(mockListener).shouldProcess(report);
        verify(mockListener, never()).shouldAwaitUserConfirmation();

        verify(mockListener, never()).getErrorAttachments(report);
        verify(mockChannel, never()).enqueue(any(Log.class), eq(crashes.getGroupName()));
    }

    @Test
    public void queuePendingCrashesAlwaysSend() throws IOException, ClassNotFoundException, JSONException {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);

        ErrorAttachmentLog mockAttachment = mock(ErrorAttachmentLog.class);
        when(mockAttachment.getId()).thenReturn(UUID.randomUUID());
        when(mockAttachment.getErrorId()).thenReturn(UUID.randomUUID());
        when(mockAttachment.getContentType()).thenReturn("");
        when(mockAttachment.getFileName()).thenReturn("");
        when(mockAttachment.getData()).thenReturn(new byte[0]);
        when(mockAttachment.isValid()).thenReturn(true);
        List<ErrorAttachmentLog> errorAttachmentLogList = Arrays.asList(mockAttachment, mockAttachment);

        ErrorReport report = new ErrorReport();

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(report);
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(new RuntimeException());
        when(StorageHelper.PreferencesStorage.getBoolean(eq(Crashes.PREF_KEY_ALWAYS_SEND), anyBoolean())).thenReturn(true);

        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(report)).thenReturn(true);

        when(mockListener.shouldProcess(report)).thenReturn(true);
        when(mockListener.shouldAwaitUserConfirmation()).thenReturn(false);

        when(mockListener.getErrorAttachments(report)).thenReturn(errorAttachmentLogList);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mErrorLog);

        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mockContext, "", mockChannel);

        verify(mockListener).shouldProcess(report);
        verify(mockListener, never()).shouldAwaitUserConfirmation();

        verify(mockListener).getErrorAttachments(report);
        verify(mockChannel).enqueue(argThat(new ArgumentMatcher<Log>() {
            @Override
            public boolean matches(Object log) {
                return log.equals(mErrorLog);
            }
        }), eq(crashes.getGroupName()));

        verify(mockChannel, times(errorAttachmentLogList.size())).enqueue(mockAttachment, crashes.getGroupName());
    }

    @Test
    public void processPendingErrorsCorrupted() throws JSONException {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");

        Crashes crashes = Crashes.getInstance();

        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mock(ManagedErrorLog.class));
        crashes.setLogSerializer(logSerializer);

        CrashesListener listener = mock(CrashesListener.class);
        crashes.setInstanceListener(listener);

        Channel channel = mock(Channel.class);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", channel);
        verifyZeroInteractions(listener);
        verify(channel, never()).enqueue(any(Log.class), anyString());
    }

    @Test
    public void disabledDuringProcessPendingErrors() throws IOException, ClassNotFoundException, JSONException {
        ErrorReport errorReport = ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION);

        File errorStorageDirectory = mock(File.class);
        when(errorStorageDirectory.listFiles()).thenReturn(new File[0]);
        CrashesListener listener = mock(CrashesListener.class);
        when(listener.shouldProcess(errorReport)).thenReturn(true);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mErrorLog);
        Channel channel = mock(Channel.class);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class), mock(File.class)}).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(errorStorageDirectory);
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION)).thenReturn(errorReport);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(EXCEPTION);
        when(StorageHelper.InternalStorage.read(any(File.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Crashes.setEnabled(false);
                return "";
            }
        });

        /* Disabled while Crashes service is processing pending errors. */
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(listener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", channel);

        verify(channel, never()).enqueue(any(Log.class), anyString());
        verify(listener).shouldProcess(errorReport);
        verifyNoMoreInteractions(listener);

        /* Disabled right before handling user confirmation. */
        Crashes.setEnabled(true);
        Crashes.unsetInstance();
        crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(listener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", channel);

        verify(channel, never()).enqueue(any(Log.class), anyString());
        verify(listener, times(2)).shouldProcess(errorReport);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void disabledDuringHandleUserConfirmation() throws IOException, ClassNotFoundException, JSONException {
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0, true);
        ErrorReport errorReport1 = ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION);
        ErrorReport errorReport2 = ErrorLogHelper.getErrorReportFromErrorLog(errorLog, EXCEPTION);

        File errorStorageDirectory = mock(File.class);
        when(errorStorageDirectory.listFiles()).thenReturn(new File[0]);
        CrashesListener listener = mock(CrashesListener.class);
        when(listener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mErrorLog).thenReturn(errorLog);
        Channel channel = mock(Channel.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Crashes.setEnabled(false);
                return null;
            }
        }).when(channel).enqueue(any(Log.class), anyString());

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class), mock(File.class)});
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(errorStorageDirectory);
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION)).thenReturn(errorReport1);
        when(ErrorLogHelper.getErrorReportFromErrorLog(errorLog, EXCEPTION)).thenReturn(errorReport2);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(EXCEPTION);
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");

        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(listener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", channel);

        verify(listener, times(2)).shouldProcess(any(ErrorReport.class));
        verify(listener).shouldAwaitUserConfirmation();
        verify(channel).enqueue(any(Log.class), anyString());

        verify(listener).getErrorAttachments(any(ErrorReport.class));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void noQueueingWhenDisabled() {
        when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(false);
        Channel channel = mock(Channel.class);
        Crashes crashes = Crashes.getInstance();
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", channel);
        verify(channel, never()).enqueue(any(Log.class), anyString());
    }

    @Test
    public void noQueueNullLog() throws JSONException {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(null);
        crashes.setLogSerializer(logSerializer);

        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mockContext, "", mockChannel);

        verify(mockChannel, never()).enqueue(any(Log.class), anyString());
    }

    @Test
    public void printErrorOnJSONException() throws JSONException {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        JSONException jsonException = new JSONException("Fake JSON exception");

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");
        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);

        when(logSerializer.deserializeLog(anyString())).thenThrow(jsonException);
        crashes.setLogSerializer(logSerializer);

        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mockContext, "", mockChannel);

        verify(mockChannel, never()).enqueue(any(Log.class), anyString());

        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(jsonException));
    }

    @Test(expected = TestCrashException.class)
    public void generateTestCrashInDebug() {
        Constants.APPLICATION_DEBUGGABLE = true;
        Crashes.generateTestCrash();
    }

    @Test
    public void generateTestCrashInRelease() {
        Constants.APPLICATION_DEBUGGABLE = false;
        Crashes.generateTestCrash();
    }

    @Test
    public void trackException() {
        /* Track exception test. */
        Crashes crashes = Crashes.getInstance();
        Channel mockChannel = mock(Channel.class);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mockChannel);
        Crashes.trackException(EXCEPTION);
        verify(mockChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof ManagedErrorLog && EXCEPTION.getMessage().equals(((ManagedErrorLog) item).getException().getMessage());
            }
        }), eq(crashes.getGroupName()));

        ManagedErrorLog mockLog = mock(ManagedErrorLog.class);
        when(mockLog.getFatal()).thenReturn(false);
        CrashesListener mockListener = mock(CrashesListener.class);
        crashes.setInstanceListener(mockListener);

        /* Crashes callback test for trackException. */
        crashes.getChannelListener().onBeforeSending(mockLog);
        verify(mockListener, never()).onBeforeSending(any(ErrorReport.class));
        crashes.getChannelListener().onSuccess(mockLog);
        verify(mockListener, never()).onSendingSucceeded(any(ErrorReport.class));
        crashes.getChannelListener().onFailure(mockLog, EXCEPTION);
        verify(mockListener, never()).onSendingFailed(any(ErrorReport.class), eq(EXCEPTION));

        ErrorAttachmentLog attachmentLog = mock(ErrorAttachmentLog.class);
        crashes.getChannelListener().onBeforeSending(attachmentLog);
        verify(mockListener, never()).onBeforeSending(any(ErrorReport.class));
        crashes.getChannelListener().onSuccess(attachmentLog);
        verify(mockListener, never()).onSendingSucceeded(any(ErrorReport.class));
        crashes.getChannelListener().onFailure(attachmentLog, EXCEPTION);
        verify(mockListener, never()).onSendingFailed(any(ErrorReport.class), eq(EXCEPTION));
    }

    @Test
    public void trackExceptionForWrapperSdk() {
        StackFrame frame = new StackFrame();
        frame.setClassName("1");
        frame.setFileName("2");
        frame.setLineNumber(3);
        frame.setMethodName("4");
        final com.microsoft.azure.mobile.crashes.ingestion.models.Exception exception = new com.microsoft.azure.mobile.crashes.ingestion.models.Exception();
        exception.setType("5");
        exception.setMessage("6");
        exception.setFrames(singletonList(frame));

        Crashes crashes = Crashes.getInstance();
        Channel mockChannel = mock(Channel.class);

        Crashes.getInstance().trackException(exception);
        verify(mockChannel, never()).enqueue(any(Log.class), eq(crashes.getGroupName()));
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mockChannel);
        Crashes.getInstance().trackException(exception);
        verify(mockChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                return item instanceof ManagedErrorLog && exception.equals(((ManagedErrorLog) item).getException());
            }
        }), eq(crashes.getGroupName()));
    }

    @Test
    public void getChannelListener() throws IOException, ClassNotFoundException {
        ErrorReport errorReport = ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION)).thenReturn(errorReport);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(EXCEPTION);

        CrashesListener crashesListener = mock(CrashesListener.class);
        Crashes.setListener(crashesListener);
        Crashes crashes = Crashes.getInstance();
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));

        ArgumentCaptor<ErrorReport> errorReportCaptor = ArgumentCaptor.forClass(ErrorReport.class);
        Channel.GroupListener channelListener = crashes.getChannelListener();
        channelListener.onBeforeSending(mErrorLog);
        verify(crashesListener).onBeforeSending(errorReportCaptor.capture());
        assertErrorEquals(mErrorLog, errorReportCaptor.getValue());
        channelListener.onSuccess(mErrorLog);
        verify(crashesListener).onSendingSucceeded(errorReportCaptor.capture());
        assertErrorEquals(mErrorLog, errorReportCaptor.getValue());
        channelListener.onFailure(mErrorLog, EXCEPTION);
        verify(crashesListener).onSendingFailed(errorReportCaptor.capture(), eq(EXCEPTION));
        assertErrorEquals(mErrorLog, errorReportCaptor.getValue());
    }

    @Test
    public void getChannelListenerErrors() throws IOException, ClassNotFoundException {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(null);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(null);

        CrashesListener mockListener = mock(CrashesListener.class);
        Crashes crashes = Crashes.getInstance();
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));

        Channel.GroupListener listener = Crashes.getInstance().getChannelListener();

        listener.onBeforeSending(mErrorLog);
        verifyStatic();
        MobileCenterLog.warn(eq(Crashes.LOG_TAG), anyString());
        Mockito.verifyNoMoreInteractions(mockListener);

        listener.onSuccess(mock(Log.class));
        verifyStatic();
        MobileCenterLog.warn(eq(Crashes.LOG_TAG), contains(Log.class.getName()));
        Mockito.verifyNoMoreInteractions(mockListener);
    }

    @Test
    public void handleUserConfirmationDoNotSend() throws IOException, ClassNotFoundException, JSONException {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(new ErrorReport());
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(null);

        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(mockListener.shouldAwaitUserConfirmation()).thenReturn(true);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mErrorLog);

        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));

        Crashes.notifyUserConfirmation(Crashes.DONT_SEND);

        verify(mockListener, never()).getErrorAttachments(any(ErrorReport.class));

        verifyStatic();
        ErrorLogHelper.removeStoredErrorLogFile(mErrorLog.getId());
        verifyStatic();
        ErrorLogHelper.removeStoredThrowableFile(mErrorLog.getId());
    }

    @Test
    public void handleUserConfirmationAlwaysSend() throws IOException, ClassNotFoundException, JSONException {

        /* Simulate the method is called from Worker Thread. */
        mockStatic(Looper.class);
        when(Looper.myLooper()).thenReturn(mock(Looper.class));
        when(Looper.getMainLooper()).thenReturn(null);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(null);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(null);

        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mErrorLog);

        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));

        Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);

        verifyStatic();
        StorageHelper.PreferencesStorage.putBoolean(Crashes.PREF_KEY_ALWAYS_SEND, true);
    }

    @Test
    public void buildErrorReport() throws IOException, ClassNotFoundException {
        ErrorReport errorReport = ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class)).thenReturn(null);
        when(ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, EXCEPTION)).thenReturn(errorReport);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(EXCEPTION);

        Crashes crashes = Crashes.getInstance();
        ErrorReport report = crashes.buildErrorReport(mErrorLog);
        assertErrorEquals(mErrorLog, report);

        mErrorLog.setId(UUIDUtils.randomUUID());
        report = crashes.buildErrorReport(mErrorLog);
        assertNull(report);
    }

    @Test
    public void buildErrorReportError() throws IOException, ClassNotFoundException {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(null);

        Exception classNotFoundException = mock(ClassNotFoundException.class);
        Exception ioException = mock(IOException.class);
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenThrow(classNotFoundException).thenThrow(ioException);

        Crashes crashes = Crashes.getInstance();

        ErrorReport report = crashes.buildErrorReport(mErrorLog);
        assertNull(report);
        report = crashes.buildErrorReport(mErrorLog);
        assertNull(report);

        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(classNotFoundException));
        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(ioException));
    }

    @Test
    public void defaultErrorReportingListener() {
        Crashes crashes = Crashes.getInstance();
        CrashesListener defaultListener = crashes.getInstanceListener();
        crashes.setInstanceListener(new CrashesListener() {
            @Override
            public boolean shouldProcess(ErrorReport report) {
                return false;
            }

            @Override
            public boolean shouldAwaitUserConfirmation() {
                return false;
            }

            @Override
            public Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report) {
                return null;
            }

            @Override
            public void onBeforeSending(ErrorReport report) {
            }

            @Override
            public void onSendingFailed(ErrorReport report, Exception e) {
            }

            @Override
            public void onSendingSucceeded(ErrorReport report) {
            }
        });

        /* Verify crashes has default listener when null is assigned. */
        crashes.setInstanceListener(null);
        CrashesListener listener = crashes.getInstanceListener();
        assertEquals(defaultListener, listener);

        /* Verify default behavior. */
        assertTrue(defaultListener.shouldProcess(null));
        assertFalse(defaultListener.shouldAwaitUserConfirmation());

        /* Nothing to verify. */
        defaultListener.getErrorAttachments(null);
        defaultListener.onBeforeSending(null);
        defaultListener.onSendingSucceeded(null);
        defaultListener.onSendingFailed(null, null);
    }

    @Test
    public void noCrashInLastSession() {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(null);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        assertNull(Crashes.getLastSessionCrashReport().get());
        Crashes.getInstance().onStarted(mock(Context.class), "", mock(Channel.class));
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());
        verifyStatic(never());
        MobileCenterLog.debug(anyString(), anyString());
    }

    @Test
    public void crashInLastSession() throws JSONException, IOException, ClassNotFoundException {
        final int tOffset = 10;
        final long appLaunchTOffset = 100L;

        final ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());
        errorLog.setErrorThreadName(Thread.currentThread().getName());
        errorLog.setToffset(tOffset);

        errorLog.setAppLaunchTOffset(appLaunchTOffset);
        errorLog.setDevice(mock(Device.class));

        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(errorLog);

        final Throwable throwable = mock(Throwable.class);
        final ErrorReport errorReport = ErrorLogHelper.getErrorReportFromErrorLog(errorLog, throwable);

        mockStatic(ErrorLogHelper.class);
        File lastErrorLogFile = errorStorageDirectory.newFile("last-error-log.json");
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(lastErrorLogFile);
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(errorStorageDirectory.newFile());
        when(ErrorLogHelper.getErrorReportFromErrorLog(errorLog, throwable)).thenReturn(errorReport);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{lastErrorLogFile});
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(throwable);

        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        assertFalse(Crashes.hasCrashedInLastSession().get());

        /*
         * Last session error is only fetched upon initialization: enabled and channel ready.
         * Here the service is disabled by default until stated, we are waiting channel to be ready, simulate that.
         */
        assertFalse(Crashes.isEnabled().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());

        @SuppressWarnings("unchecked")
        SimpleConsumer<ErrorReport> beforeCallback = (SimpleConsumer<ErrorReport>) mock(SimpleConsumer.class);
        Crashes.getLastSessionCrashReport().thenAccept(beforeCallback);
        verify(beforeCallback).apply(null);

        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));
        assertTrue(Crashes.isEnabled().get());
        assertTrue(Crashes.hasCrashedInLastSession().get());

        @SuppressWarnings("unchecked")
        SimpleConsumer<ErrorReport> afterCallback = (SimpleConsumer<ErrorReport>) mock(SimpleConsumer.class);
        SimpleFuture<ErrorReport> future = Crashes.getLastSessionCrashReport();
        future.thenAccept(afterCallback);
        future.thenAccept(afterCallback);
        ArgumentCaptor<ErrorReport> errorReportCaptor = ArgumentCaptor.forClass(ErrorReport.class);
        verify(afterCallback, times(2)).apply(errorReportCaptor.capture());
        assertEquals(errorReportCaptor.getAllValues().get(0), errorReportCaptor.getAllValues().get(1));
        ErrorReport result = errorReportCaptor.getValue();
        assertNotNull(result);
        assertEquals(errorLog.getId().toString(), result.getId());
        assertEquals(errorLog.getErrorThreadName(), result.getThreadName());
        assertEquals(new Date(tOffset - appLaunchTOffset), result.getAppStartTime());
        assertEquals(new Date(tOffset), result.getAppErrorTime());
        assertNotNull(result.getDevice());
        assertEquals(throwable, result.getThrowable());
    }

    @Test
    public void noCrashInLastSessionWhenDisabled() {

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(errorStorageDirectory.getRoot());

        Crashes.setEnabled(false);

        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());

        verifyStatic(never());
        ErrorLogHelper.getLastErrorLogFile();
    }

    @Test
    public void crashInLastSessionError() throws JSONException, IOException, ClassNotFoundException {
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(mock(ManagedErrorLog.class));

        mockStatic(ErrorLogHelper.class);
        File lastErrorLogFile = errorStorageDirectory.newFile("last-error-log.json");
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(lastErrorLogFile);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{lastErrorLogFile});
        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");

        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);

        assertFalse(Crashes.hasCrashedInLastSession().get());

        JSONException jsonException = new JSONException("Fake JSON exception");
        when(logSerializer.deserializeLog(anyString())).thenThrow(jsonException);

        /*
         * Last session error is only fetched upon initialization: enabled and channel ready.
         * Here the service is disabled by default until started, we are waiting channel to be ready, simulate that.
         */
        assertFalse(Crashes.isEnabled().get());
        assertNull(Crashes.getLastSessionCrashReport().get());
        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));
        assertTrue(Crashes.isEnabled().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());

        /*
         * De-serializing fails twice: processing the log from last time as part of the bulk processing.
         * And loading that same file for exposing it in getLastErrorReport.
         */
        verifyStatic(times(2));
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(jsonException));
    }

    @Test
    public void crashInLastSessionCorrupted() throws IOException {
        mockStatic(ErrorLogHelper.class);
        File file = errorStorageDirectory.newFile("last-error-log.json");
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{file});
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(file);
        Crashes.getInstance().onStarted(mock(Context.class), "", mock(Channel.class));
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());
    }

    @Test
    public void getLastSessionCrashReportWithMultipleListeners() {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(null);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);

        @SuppressWarnings("unchecked")
        SimpleConsumer<ErrorReport> callback = (SimpleConsumer<ErrorReport>) mock(SimpleConsumer.class);

        /* Call twice for multiple callbacks before initialize. */
        Crashes.getLastSessionCrashReport().thenAccept(callback);
        Crashes.getLastSessionCrashReport().thenAccept(callback);
        Crashes.getInstance().onStarted(mock(Context.class), "", mock(Channel.class));
        assertFalse(Crashes.hasCrashedInLastSession().get());
        verify(callback, times(2)).apply(null);
    }

    @Test
    public void setWrapperSdkListener() {
        mockStatic(ErrorLogHelper.class);
        ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());
        when(ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Throwable.class), anyMapOf(Thread.class, StackTraceElement[].class), anyLong(), anyBoolean())).thenReturn(errorLog);
        Crashes.getInstance().setLogSerializer(mock(LogSerializer.class));
        Crashes.WrapperSdkListener wrapperSdkListener = mock(Crashes.WrapperSdkListener.class);
        Crashes.getInstance().setWrapperSdkListener(wrapperSdkListener);
        Crashes.getInstance().saveUncaughtException(Thread.currentThread(), new TestCrashException());
        verify(wrapperSdkListener).onCrashCaptured(errorLog);
    }

    @Test
    public void saveWrapperSdkErrorLogJSONException() throws JSONException {
        mockStatic(MobileCenterLog.class);
        ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(errorLog)).thenThrow(new JSONException("mock"));
        Crashes.getInstance().setLogSerializer(logSerializer);
        WrapperSdkExceptionManager.saveWrapperSdkErrorLog(errorLog);
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString(), any(JSONException.class));
    }

    @Test
    public void saveWrapperSdkErrorLogIOException() throws IOException, JSONException {
        mockStatic(MobileCenterLog.class);
        ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());
        mockStatic(StorageHelper.InternalStorage.class);
        doThrow(new IOException()).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.write(any(File.class), anyString());
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(errorLog)).thenReturn("mock");
        Crashes.getInstance().setLogSerializer(logSerializer);
        WrapperSdkExceptionManager.saveWrapperSdkErrorLog(errorLog);
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    public void sendMoreThan2ErrorAttachments() throws IOException, ClassNotFoundException, JSONException {
        int MAX_ATTACHMENT_PER_CRASH = 2;
        int numOfAttachments = MAX_ATTACHMENT_PER_CRASH + 1;

        ArrayList<ErrorAttachmentLog> errorAttachmentLogs = new ArrayList<>(3);
        for(int i = 0; i < numOfAttachments; ++i) {
            ErrorAttachmentLog log = mock(ErrorAttachmentLog.class);
            when(log.isValid()).thenReturn(true);
            errorAttachmentLogs.add(log);
        }

        CrashesListener listener = mock(CrashesListener.class);
        when(listener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(listener.getErrorAttachments(any(ErrorReport.class))).thenReturn(errorAttachmentLogs);

        ManagedErrorLog log = mock(ManagedErrorLog.class);
        when(log.getId()).thenReturn(UUID.randomUUID());

        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(log);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(mock(File.class));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), any(Throwable.class))).thenReturn(new ErrorReport());

        when(StorageHelper.InternalStorage.read(any(File.class))).thenReturn("");
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(mock(Throwable.class));

        Crashes crashes = Crashes.getInstance();
        crashes.setInstanceListener(listener);
        crashes.setLogSerializer(logSerializer);

        crashes.onStarting(mMobileCenterHandler);
        crashes.onStarted(mock(Context.class), "", mock(Channel.class));

        String expectedMessage = "A limit of " + MAX_ATTACHMENT_PER_CRASH + " attachments per error report might be enforced by server.";
        PowerMockito.verifyStatic();
        MobileCenterLog.warn(Crashes.LOG_TAG, expectedMessage);
    }
}
