package com.microsoft.azure.mobile.crashes;

import android.content.Context;
import android.os.SystemClock;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.MobileCenterHandler;
import com.microsoft.azure.mobile.crashes.ingestion.models.Exception;
import com.microsoft.azure.mobile.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.DeviceInfoHelper;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.ShutdownHelper;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@PrepareForTest({SystemClock.class, StorageHelper.PreferencesStorage.class, StorageHelper.InternalStorage.class, Crashes.class, ErrorLogHelper.class, DeviceInfoHelper.class, ShutdownHelper.class, MobileCenterLog.class, MobileCenter.class, HandlerUtils.class})
public class UncaughtExceptionHandlerTest {

    private static final String CRASHES_ENABLED_KEY = KEY_ENABLED + "_" + Crashes.getInstance().getServiceName();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    private UncaughtExceptionHandler mExceptionHandler;

    @Before
    public void setUp() throws java.lang.Exception {
        Crashes.unsetInstance();
        mockStatic(MobileCenter.class);
        mockStatic(MobileCenterLog.class);
        mockStatic(SystemClock.class);
        mockStatic(StorageHelper.PreferencesStorage.class);
        mockStatic(StorageHelper.InternalStorage.class);
        mockStatic(ErrorLogHelper.class);
        mockStatic(DeviceInfoHelper.class);
        mockStatic(System.class);

        @SuppressWarnings("unchecked")
        MobileCenterFuture<Boolean> future = (MobileCenterFuture<Boolean>) mock(MobileCenterFuture.class);
        when(MobileCenter.isEnabled()).thenReturn(future);
        when(future.get()).thenReturn(true);
        when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                Mockito.when(StorageHelper.PreferencesStorage.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(CRASHES_ENABLED_KEY), anyBoolean());

        ManagedErrorLog errorLogMock = mock(ManagedErrorLog.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(new File("."));
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), Matchers.<Map<Thread, StackTraceElement[]>>any(), anyLong(), anyBoolean()))
                .thenReturn(errorLogMock);

        when(errorLogMock.getId()).thenReturn(UUID.randomUUID());

        mDefaultExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(mDefaultExceptionHandler);
        mExceptionHandler = new UncaughtExceptionHandler();

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
        MobileCenterHandler handler = mock(MobileCenterHandler.class);
        Crashes.getInstance().onStarting(handler);
        doAnswer(runNow).when(handler).post(any(Runnable.class), any(Runnable.class));
    }

    @Test
    public void registerWorks() {

        /* Verify that exception handler is default */
        assertEquals(mDefaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
        mExceptionHandler.register();

        /* Verify that creation registers handler and previously defined handler is correctly saved */
        assertEquals(mExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
        assertEquals(mDefaultExceptionHandler, mExceptionHandler.getDefaultUncaughtExceptionHandler());

        mExceptionHandler.unregister();
        assertEquals(mDefaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());

        mExceptionHandler.setIgnoreDefaultExceptionHandler(true);
        mExceptionHandler.register();
        assertEquals(mExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
        assertNull(mExceptionHandler.getDefaultUncaughtExceptionHandler());
    }

    @Test
    public void handleExceptionAndPassOn() {
        mExceptionHandler.register();

        // Verify that the exception is being handled and passed on to the previously defined UncaughtExceptionHandler
        Thread thread = Thread.currentThread();
        RuntimeException exception = new RuntimeException();
        mExceptionHandler.uncaughtException(thread, exception);
        verify(mDefaultExceptionHandler).uncaughtException(thread, exception);

        verifyStatic();
        ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), Matchers.<Map<Thread, StackTraceElement[]>>any(), anyLong(), anyBoolean());
    }

    @Test
    public void handleExceptionAndIgnoreDefaultHandler() {

        /* Register crash handler */
        mExceptionHandler.register();

        /* Verify that the exception is handled and not being passed on to the previous default UncaughtExceptionHandler */
        Thread thread = Thread.currentThread();
        RuntimeException exception = new RuntimeException();
        mExceptionHandler.setIgnoreDefaultExceptionHandler(true);
        mExceptionHandler.uncaughtException(thread, exception);
        verifyNoMoreInteractions(mDefaultExceptionHandler);

        verifyStatic();
        ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), Matchers.<Map<Thread, StackTraceElement[]>>any(), anyLong(), anyBoolean());
        verifyStatic();
        System.exit(10);
    }

    @Test
    public void passDefaultHandler() {
        /* Verify that when crashes is disabled, an exception is instantly passed on */
        when(Crashes.isEnabled().get()).thenReturn(false);

        mExceptionHandler.register();
        mExceptionHandler.setIgnoreDefaultExceptionHandler(false);

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        mExceptionHandler.uncaughtException(thread, exception);
        verify(mDefaultExceptionHandler).uncaughtException(thread, exception);

        PowerMockito.verifyNoMoreInteractions(ErrorLogHelper.class);
    }

    @Test
    public void crashesDisabledNoDefaultHandler() {
        /* Verify that when crashes is disabled, an exception is instantly passed on */
        when(Crashes.isEnabled().get()).thenReturn(false);

        mExceptionHandler.register();
        mExceptionHandler.setIgnoreDefaultExceptionHandler(true);

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        mExceptionHandler.uncaughtException(thread, exception);
        verifyNoMoreInteractions(mDefaultExceptionHandler);
        PowerMockito.verifyNoMoreInteractions(ErrorLogHelper.class);
    }

    @Test
    public void testInvalidJsonException() throws JSONException {
        mExceptionHandler.register();

        LogSerializer logSerializer = mock(LogSerializer.class);
        final JSONException jsonException = new JSONException("Fake JSON serializing exception");
        when(logSerializer.serializeLog(any(Log.class))).thenThrow(jsonException);

        Whitebox.setInternalState(Crashes.getInstance(), "mLogSerializer", logSerializer);

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        mExceptionHandler.uncaughtException(thread, exception);

        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(jsonException));

        verify(mDefaultExceptionHandler).uncaughtException(thread, exception);
    }

    @Test
    public void testIOException() throws java.lang.Exception {
        mExceptionHandler.register();

        IOException ioException = new IOException("Fake IO exception");
        PowerMockito.doThrow(ioException).when(StorageHelper.InternalStorage.class);
        StorageHelper.InternalStorage.write(any(File.class), anyString());

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        mExceptionHandler.uncaughtException(thread, exception);

        verifyStatic();
        MobileCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(ioException));

        verify(mDefaultExceptionHandler).uncaughtException(thread, exception);
    }
}
