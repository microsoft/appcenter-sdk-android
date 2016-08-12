package avalanche.errors;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.PrefStorageConstants;
import avalanche.core.utils.StorageHelper;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.utils.ErrorLogHelper;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemClock.class, StorageHelper.class, System.class, Process.class, ErrorReporting.class, ErrorLogHelper.class, DeviceInfoHelper.class, UncaughtExceptionHandler.ShutdownHelper.class})
public class UncaughtExceptionHandlerTest {

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;
    private UncaughtExceptionHandler exceptionHandler;

    @Before
    public void setUp() {
        ErrorReporting.unsetInstance();
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
        // Verify that creation registers handler and previously defined handler is correctly saved
        assertEquals(exceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
        assertEquals(defaultExceptionHandler, exceptionHandler.getDefaultUncaughtExceptionHandler());
    }

    @Test
    public void unregisterWorks() {
        exceptionHandler.unregister();
        assertEquals(defaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
    }

    @Test
    public void handleExceptionAndPassOn() {
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
    public void passOnException() {
        // Verify that when error reporting is disabled, an exception is instantly passed on 
        when(ErrorReporting.isEnabled()).thenReturn(false);

        Thread thread = Thread.currentThread();
        RuntimeException exception = new RuntimeException();
        exceptionHandler.uncaughtException(thread, exception);
        verify(defaultExceptionHandler).uncaughtException(thread, exception);

        PowerMockito.verifyNoMoreInteractions(ErrorLogHelper.class);
    }

}
