package avalanche.errors;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;

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
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.utils.PrefStorageConstants;
import avalanche.core.utils.StorageHelper;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.errors.model.ErrorReport;
import avalanche.errors.model.TestCrashException;
import avalanche.errors.utils.ErrorLogHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ErrorLogHelper.class, SystemClock.class, StorageHelper.InternalStorage.class, StorageHelper.PreferencesStorage.class})
public class ErrorReportingTest {

    @Before
    public void setUp() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        ErrorReporting.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(StorageHelper.InternalStorage.class);
        mockStatic(StorageHelper.PreferencesStorage.class);

        when(SystemClock.elapsedRealtime()).thenReturn(System.currentTimeMillis());

        final String key = PrefStorageConstants.KEY_ENABLED + "_" + ErrorReporting.getInstance().getGroupName();
        when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                Mockito.when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(enabled);
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
    public void checkFactories() {
        Map<String, LogFactory> factories = ErrorReporting.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(JavaErrorLog.TYPE) instanceof JavaErrorLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void setEnabled() {
        Context mockContext = mock(Context.class);
        AvalancheChannel mockChannel = mock(AvalancheChannel.class);

        ErrorReporting.getInstance().onChannelReady(mockContext, mockChannel);

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
    public void queuePendingCrashes() throws IOException, ClassNotFoundException {
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
        errorReporting.setLogSerializer(new LogSerializer() {
            @Override
            public String serializeLog(@NonNull Log log) throws JSONException {
                return null;
            }

            @Override
            public Log deserializeLog(@NonNull String json) throws JSONException {
                return errorLog;
            }

            @Override
            public String serializeContainer(@NonNull LogContainer container) throws JSONException {
                return null;
            }

            @Override
            public LogContainer deserializeContainer(@NonNull String json) throws JSONException {
                return null;
            }

            @Override
            public void addLogFactory(@NonNull String logType, @NonNull LogFactory logFactory) {

            }
        });
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
    public void noQueueingWhenDisabled() {
        Context mockContext = mock(Context.class);
        AvalancheChannel mockChannel = mock(AvalancheChannel.class);

        ErrorReporting.setEnabled(false);
        ErrorReporting errorReporting = ErrorReporting.getInstance();

        errorReporting.onChannelReady(mockContext, mockChannel);

        mockStatic(ErrorLogHelper.class);

        verifyNoMoreInteractions(ErrorLogHelper.class);
    }

    @Test(expected = TestCrashException.class)
    public void generateTestCrash() {
        ErrorReporting.generateTestCrash();
    }
}
