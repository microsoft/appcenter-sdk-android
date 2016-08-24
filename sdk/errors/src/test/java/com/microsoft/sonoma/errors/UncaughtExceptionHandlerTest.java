package com.microsoft.sonoma.errors;

import android.content.Context;
import android.os.SystemClock;

import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.json.LogSerializer;
import com.microsoft.sonoma.core.utils.DeviceInfoHelper;
import com.microsoft.sonoma.core.utils.PrefStorageConstants;
import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.errors.ingestion.models.JavaErrorLog;
import com.microsoft.sonoma.errors.utils.ErrorLogHelper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemClock.class, StorageHelper.class, ErrorReporting.class, ErrorLogHelper.class, DeviceInfoHelper.class, UncaughtExceptionHandler.ShutdownHelper.class, SonomaLog.class})
public class UncaughtExceptionHandlerTest {

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;
    private UncaughtExceptionHandler exceptionHandler;

    @Before
    public void setUp() {
        ErrorReporting.unsetInstance();
        mockStatic(SonomaLog.class);
        mockStatic(SystemClock.class);
        mockStatic(StorageHelper.PreferencesStorage.class);
        mockStatic(StorageHelper.InternalStorage.class);
        mockStatic(ErrorLogHelper.class);
        mockStatic(DeviceInfoHelper.class);
        mockStatic(UncaughtExceptionHandler.ShutdownHelper.class);

        Context mockContext = mock(Context.class);

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

        JavaErrorLog errorLogMock = mock(JavaErrorLog.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(new File("."));
        when(ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Throwable.class), Matchers.<Map<Thread, StackTraceElement[]>>any(), anyLong()))
                .thenReturn(errorLogMock);

        when(errorLogMock.getId()).thenReturn(UUID.randomUUID());


        defaultExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler);
        exceptionHandler = new UncaughtExceptionHandler(mockContext);
    }

    @Test
    public void registerWorks() {
        // Verify that exception handler is default
        assertEquals(defaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
        exceptionHandler.register();
        // Verify that creation registers handler and previously defined handler is correctly saved
        assertEquals(exceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
        assertEquals(defaultExceptionHandler, exceptionHandler.getDefaultUncaughtExceptionHandler());

        exceptionHandler.unregister();
        assertEquals(defaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());

        exceptionHandler.setIgnoreDefaultExceptionHandler(true);
        exceptionHandler.register();
        assertEquals(exceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
        assertNull(exceptionHandler.getDefaultUncaughtExceptionHandler());
    }

    @Test
    public void handleExceptionAndPassOn() {
        exceptionHandler.register();

        // Verify that the exception is being handled and passed on to the previously defined UncaughtExceptionHandler
        Thread thread = Thread.currentThread();
        RuntimeException exception = new RuntimeException();
        exceptionHandler.uncaughtException(thread, exception);
        verify(defaultExceptionHandler).uncaughtException(thread, exception);

        verifyStatic();
        ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Throwable.class), Matchers.<Map<Thread, StackTraceElement[]>>any(), anyLong());
    }

    @Test
    public void handleExceptionAndIgnoreDefaultHandler() {
        exceptionHandler.register();

        // Verify that the exception is handled and not being passed on to the previous default UncaughtExceptionHandler
        Thread thread = Thread.currentThread();
        RuntimeException exception = new RuntimeException();
        exceptionHandler.setIgnoreDefaultExceptionHandler(true);
        exceptionHandler.uncaughtException(thread, exception);
        verifyNoMoreInteractions(defaultExceptionHandler);

        verifyStatic();
        ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Throwable.class), Matchers.<Map<Thread, StackTraceElement[]>>any(), anyLong());
        UncaughtExceptionHandler.ShutdownHelper.shutdown();
    }

    @Test
    public void passDefaultHandler() {
        // Verify that when error reporting is disabled, an exception is instantly passed on
        when(ErrorReporting.isEnabled()).thenReturn(false);

        exceptionHandler.register();
        exceptionHandler.setIgnoreDefaultExceptionHandler(false);

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        exceptionHandler.uncaughtException(thread, exception);
        verify(defaultExceptionHandler).uncaughtException(thread, exception);

        PowerMockito.verifyNoMoreInteractions(ErrorLogHelper.class);
    }

    @Test
    public void errorReportingDisabledNoDefaultHandler() {
        // Verify that when error reporting is disabled, an exception is instantly passed on
        when(ErrorReporting.isEnabled()).thenReturn(false);

        exceptionHandler.register();
        exceptionHandler.setIgnoreDefaultExceptionHandler(true);

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        exceptionHandler.uncaughtException(thread, exception);
        verifyNoMoreInteractions(defaultExceptionHandler);
        PowerMockito.verifyNoMoreInteractions(ErrorLogHelper.class);
    }

    @Test
    public void testInvalidJsonException() throws JSONException {
        exceptionHandler.register();

        LogSerializer logSerializer = mock(LogSerializer.class);
        final JSONException jsonException = new JSONException("Fake JSON serializing exception");
        when(logSerializer.serializeLog(any(Log.class))).thenThrow(jsonException);

        Whitebox.setInternalState(exceptionHandler, "mLogSerializer", logSerializer);

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        exceptionHandler.uncaughtException(thread, exception);

        verifyStatic();
        SonomaLog.error(anyString(), eq(jsonException));

        verify(defaultExceptionHandler).uncaughtException(thread, exception);
    }

    @Test
    public void testIOException() throws Exception {
        exceptionHandler.register();

        IOException ioException = new IOException("Fake IO exception");
        PowerMockito.doThrow(ioException).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.write(any(File.class), anyString());

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        exceptionHandler.uncaughtException(thread, exception);

        verifyStatic();
        SonomaLog.error(anyString(), eq(ioException));

        verify(defaultExceptionHandler).uncaughtException(thread, exception);
    }
}
