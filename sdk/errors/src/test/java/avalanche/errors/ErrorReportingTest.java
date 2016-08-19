package avalanche.errors;

import android.content.Context;
import android.os.SystemClock;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.PrefStorageConstants;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.UUIDUtils;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.errors.model.ErrorAttachment;
import avalanche.errors.model.ErrorReport;
import avalanche.errors.model.TestCrashException;
import avalanche.errors.utils.ErrorLogHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ErrorLogHelper.class, SystemClock.class, StorageHelper.InternalStorage.class, StorageHelper.PreferencesStorage.class, AvalancheLog.class})
public class ErrorReportingTest {

    private static void assertErrorEquals(JavaErrorLog errorLog, Throwable throwable, ErrorReport errorReport) {
        assertNotNull(errorReport);
        assertEquals(errorLog.getId().toString(), errorReport.getId());
        assertEquals(errorLog.getErrorThreadName(), errorReport.getThreadName());
        assertEquals(throwable, errorReport.getThrowable());
        assertEquals(errorLog.getToffset() - errorLog.getAppLaunchTOffset(), errorReport.getAppStartTime().getTime());
        assertEquals(errorLog.getToffset(), errorReport.getAppErrorTime().getTime());
        assertEquals(errorLog.getDevice(), errorReport.getDevice());
    }

    @Before
    public void setUp() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        ErrorReporting.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(StorageHelper.InternalStorage.class);
        mockStatic(StorageHelper.PreferencesStorage.class);
        mockStatic(AvalancheLog.class);

        when(SystemClock.elapsedRealtime()).thenReturn(System.currentTimeMillis());

        final String key = PrefStorageConstants.KEY_ENABLED + "_" + ErrorReporting.getInstance().getGroupName();
        when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(key), anyBoolean());
    }

    @Test
    public void singleton() {
        Assert.assertSame(ErrorReporting.getInstance(), ErrorReporting.getInstance());
    }

    @Test
    public void checkConfig() {
        ErrorReporting instance = ErrorReporting.getInstance();
        Map<String, LogFactory> factories = instance.getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(JavaErrorLog.TYPE) instanceof JavaErrorLogFactory);
        assertTrue(factories.isEmpty());
        assertEquals(1, instance.getTriggerCount());
        assertEquals(ErrorReporting.ERROR_GROUP, instance.getGroupName());
    }

    @Test
    public void setEnabled() {
        ErrorReporting.getInstance().onChannelReady(mock(Context.class), mock(AvalancheChannel.class));

        assertTrue(ErrorReporting.isEnabled());
        assertTrue(ErrorReporting.getInstance().getInitializeTimestamp() > 0);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
        ErrorReporting.setEnabled(false);
        assertFalse(ErrorReporting.isEnabled());
        assertEquals(ErrorReporting.getInstance().getInitializeTimestamp(), -1);
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
        ErrorReporting.setEnabled(true);
        assertTrue(ErrorReporting.isEnabled());
        assertTrue(ErrorReporting.getInstance().getInitializeTimestamp() > 0);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
    }

    @Test
    public void queuePendingCrashesShouldProcess() throws IOException, ClassNotFoundException, JSONException {
        Context mockContext = mock(Context.class);
        AvalancheChannel mockChannel = mock(AvalancheChannel.class);

        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenReturn(new ErrorReport());

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(new RuntimeException());

        ErrorReportingListener mockListener = mock(ErrorReportingListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(mockListener.shouldAwaitUserConfirmation()).thenReturn(false);

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(errorLog);

        errorReporting.setLogSerializer(logSerializer);
		errorReporting.setInstanceListener(mockListener);
        errorReporting.onChannelReady(mockContext, mockChannel);

        verify(mockListener).shouldProcess(any(ErrorReport.class));
        verify(mockListener).shouldAwaitUserConfirmation();
        verify(mockChannel).enqueue(argThat(new ArgumentMatcher<Log>() {
            @Override
            public boolean matches(Object log) {
                return log.equals(errorLog);
            }
        }), eq(errorReporting.getGroupName()));
    }

    @Test
    public void queuePendingCrashesShouldNotProcess() throws IOException, ClassNotFoundException, JSONException {
        Context mockContext = mock(Context.class);
        AvalancheChannel mockChannel = mock(AvalancheChannel.class);

        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenReturn(new ErrorReport());

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(new RuntimeException());

        ErrorReportingListener mockListener = mock(ErrorReportingListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(false);

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(errorLog);

        errorReporting.setLogSerializer(logSerializer);
        errorReporting.setInstanceListener(mockListener);
        errorReporting.onChannelReady(mockContext, mockChannel);

        verify(mockListener).shouldProcess(any(ErrorReport.class));
        verify(mockListener, never()).shouldAwaitUserConfirmation();
        verify(mockChannel, never()).enqueue(any(Log.class), eq(errorReporting.getGroupName()));
    }

    @Test
    public void queuePendingCrashesAlwaysSend() throws IOException, ClassNotFoundException, JSONException {
        Context mockContext = mock(Context.class);
        AvalancheChannel mockChannel = mock(AvalancheChannel.class);

        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenReturn(new ErrorReport());

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(new RuntimeException());
        when(StorageHelper.PreferencesStorage.getBoolean(eq(ErrorReporting.PREF_KEY_ALWAYS_SEND), anyBoolean())).thenReturn(true);

        ErrorReportingListener mockListener = mock(ErrorReportingListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(errorLog);

        errorReporting.setLogSerializer(logSerializer);
        errorReporting.setInstanceListener(mockListener);
        errorReporting.onChannelReady(mockContext, mockChannel);

        verify(mockListener).shouldProcess(any(ErrorReport.class));
        verify(mockListener, never()).shouldAwaitUserConfirmation();
        verify(mockChannel).enqueue(argThat(new ArgumentMatcher<Log>() {
            @Override
            public boolean matches(Object log) {
                return log.equals(errorLog);
            }
        }), eq(errorReporting.getGroupName()));
    }

    @Test
    public void noQueueingWhenDisabled() {
        ErrorReporting.setEnabled(false);
        ErrorReporting errorReporting = ErrorReporting.getInstance();

        errorReporting.onChannelReady(mock(Context.class), mock(AvalancheChannel.class));

        mockStatic(ErrorLogHelper.class);

        verifyNoMoreInteractions(ErrorLogHelper.class);
    }

    @Test
    public void noQueueNullLog() throws JSONException {
        Context mockContext = mock(Context.class);
        AvalancheChannel mockChannel = mock(AvalancheChannel.class);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(null);
        errorReporting.setLogSerializer(logSerializer);

        errorReporting.onChannelReady(mockContext, mockChannel);

        verify(mockChannel, never()).enqueue(any(Log.class), anyString());
    }

    @Test
    public void printErrorOnJSONException() throws JSONException {
        Context mockContext = mock(Context.class);
        AvalancheChannel mockChannel = mock(AvalancheChannel.class);
        final JSONException jsonException = new JSONException("Fake JSON exception");

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);

        when(logSerializer.deserializeLog(anyString())).thenThrow(jsonException);
        errorReporting.setLogSerializer(logSerializer);

        errorReporting.onChannelReady(mockContext, mockChannel);

        verify(mockChannel, never()).enqueue(any(Log.class), anyString());

        verifyStatic();
        AvalancheLog.error(anyString(), eq(jsonException));
    }

    @Test(expected = TestCrashException.class)
    public void generateTestCrash() {
        ErrorReporting.generateTestCrash();
    }

    @Test
    public void getChannelListener() throws IOException, ClassNotFoundException {
        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);
        final String exceptionMessage = "This is a test exception.";
        final Exception exception = new Exception() {
            @Override
            public String getMessage() {
                return exceptionMessage;
            }
        };

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenCallRealMethod();

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(exception);

        ErrorReporting.setListener(new AbstractErrorReportingListener() {
            @Override
            public void onBeforeSending(ErrorReport errorReport) {
                assertErrorEquals(errorLog, exception, errorReport);
            }

            @Override
            public void onSendingSucceeded(ErrorReport errorReport) {
                assertErrorEquals(errorLog, exception, errorReport);
            }

            @Override
            public void onSendingFailed(ErrorReport errorReport, Exception e) {
                assertErrorEquals(errorLog, exception, errorReport);
            }
        });

        AvalancheChannel.GroupListener listener = ErrorReporting.getInstance().getChannelListener();
        listener.onBeforeSending(errorLog);
        listener.onSuccess(errorLog);
        listener.onFailure(errorLog, exception);
    }

    @Test
    public void getChannelListenerErrors() throws IOException, ClassNotFoundException {
        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenReturn(null);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(null);

        ErrorReportingListener mockListener = mock(ErrorReportingListener.class);
        ErrorReporting errorReporting = ErrorReporting.getInstance();

        errorReporting.setInstanceListener(mockListener);

        AvalancheChannel.GroupListener listener = ErrorReporting.getInstance().getChannelListener();

        listener.onBeforeSending(errorLog);
        verifyStatic();
        AvalancheLog.warn(anyString());
        Mockito.verifyNoMoreInteractions(mockListener);

        listener.onSuccess(mock(Log.class));
        verifyStatic();
        AvalancheLog.warn(contains(Log.class.getName()));
        Mockito.verifyNoMoreInteractions(mockListener);
    }

    @Test
    public void handleUserConfirmationDoNotSend() throws IOException, ClassNotFoundException, JSONException {
        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenReturn(null);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(null);

        ErrorReportingListener mockListener = mock(ErrorReportingListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(mockListener.shouldAwaitUserConfirmation()).thenReturn(true);

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(errorLog);

        errorReporting.setLogSerializer(logSerializer);
        errorReporting.setInstanceListener(mockListener);
        errorReporting.onChannelReady(mock(Context.class), mock(AvalancheChannel.class));

        ErrorReporting.notifyUserConfirmation(ErrorReporting.DONT_SEND);

        verifyStatic();
        ErrorLogHelper.removeStoredErrorLogFile(errorLog.getId());
        ErrorLogHelper.removeStoredThrowableFile(errorLog.getId());
    }

    @Test
    public void handleUserConfirmationAlwaysSend() throws IOException, ClassNotFoundException, JSONException {
        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenReturn(null);

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(null);

        ErrorReportingListener mockListener = mock(ErrorReportingListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString())).thenReturn(errorLog);

        errorReporting.setLogSerializer(logSerializer);
        errorReporting.setInstanceListener(mockListener);
        errorReporting.onChannelReady(mock(Context.class), mock(AvalancheChannel.class));

        ErrorReporting.notifyUserConfirmation(ErrorReporting.ALWAYS_SEND);

        verifyStatic();
        StorageHelper.PreferencesStorage.putBoolean(ErrorReporting.PREF_KEY_ALWAYS_SEND, true);
    }

    @Test
    public void buildErrorReport() throws IOException, ClassNotFoundException {
        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);
        final String exceptionMessage = "This is a test exception.";
        final Exception exception = new Exception() {
            @Override
            public String getMessage() {
                return exceptionMessage;
            }
        };

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File(".")).thenReturn(null);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenCallRealMethod();

        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenReturn(exception);

        ErrorReporting errorReporting = ErrorReporting.getInstance();
        ErrorReport report = errorReporting.buildErrorReport(errorLog);
        assertErrorEquals(errorLog, exception, report);

        errorLog.setId(UUIDUtils.randomUUID());
        report = errorReporting.buildErrorReport(errorLog);
        assertNull(report);
    }

    @Test
    public void buildErrorReportError() throws IOException, ClassNotFoundException {
        final JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), Thread.getAllStackTraces(), 0);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{new File(".")});
        when(ErrorLogHelper.getStoredThrowableFile(any(UUID.class))).thenReturn(new File("."));
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(JavaErrorLog.class), any(Throwable.class))).thenReturn(null);

        Exception classNotFoundException = mock(ClassNotFoundException.class);
        Exception ioException = mock(IOException.class);
        when(StorageHelper.InternalStorage.readObject(any(File.class))).thenThrow(classNotFoundException).thenThrow(ioException);

        ErrorReporting errorReporting = ErrorReporting.getInstance();

        ErrorReport report = errorReporting.buildErrorReport(errorLog);
        assertNull(report);
        report = errorReporting.buildErrorReport(errorLog);
        assertNull(report);

        verifyStatic();
        AvalancheLog.error(anyString(), eq(classNotFoundException));
        AvalancheLog.error(anyString(), eq(ioException));
    }

    @Test
    public void defaultErrorReportingListener() {
        ErrorReporting errorReporting = ErrorReporting.getInstance();
        ErrorReportingListener defaultListener = errorReporting.getInstanceListener();
        errorReporting.setInstanceListener(new ErrorReportingListener() {
            @Override
            public boolean shouldProcess(ErrorReport errorReport) {
                return false;
            }

            @Override
            public boolean shouldAwaitUserConfirmation() {
                return false;
            }

            @Override
            public ErrorAttachment getErrorAttachment(ErrorReport errorReport) {
                return null;
            }

            @Override
            public void onBeforeSending(ErrorReport errorReport) {
            }

            @Override
            public void onSendingFailed(ErrorReport errorReport, Exception e) {
            }

            @Override
            public void onSendingSucceeded(ErrorReport errorReport) {
            }
        });

        /* Verify error reporting has default listener when null is assigned. */
        errorReporting.setInstanceListener(null);
        ErrorReportingListener listener = errorReporting.getInstanceListener();
        assertEquals(defaultListener, listener);

        /* Verify default behavior. */
        assertTrue(defaultListener.shouldProcess(null));
        assertFalse(defaultListener.shouldAwaitUserConfirmation());

        /* Nothing to verify. */
        defaultListener.getErrorAttachment(null);
        defaultListener.onBeforeSending(null);
        defaultListener.onSendingSucceeded(null);
        defaultListener.onSendingFailed(null, null);
    }
}
