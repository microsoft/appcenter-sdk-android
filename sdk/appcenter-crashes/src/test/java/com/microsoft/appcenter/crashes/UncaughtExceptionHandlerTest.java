/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.os.SystemClock;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.ShutdownHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@SuppressWarnings("unused")
@PrepareForTest({
        AppCenter.class,
        AppCenterLog.class,
        Crashes.class,
        DeviceInfoHelper.class,
        ErrorLogHelper.class,
        FileManager.class,
        HandlerUtils.class,
        SharedPreferencesManager.class,
        ShutdownHelper.class,
        SystemClock.class
})
public class UncaughtExceptionHandlerTest {

    private static final String CRASHES_ENABLED_KEY = KEY_ENABLED + "_" + Crashes.getInstance().getServiceName();

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Rule
    public final TemporaryFolder mErrorStorageDirectory = new TemporaryFolder();

    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    private UncaughtExceptionHandler mExceptionHandler;

    @Before
    public void setUp() {
        Crashes.unsetInstance();
        mockStatic(AppCenter.class);
        mockStatic(AppCenterLog.class);
        mockStatic(SystemClock.class);
        mockStatic(FileManager.class);
        mockStatic(SharedPreferencesManager.class);
        mockStatic(ErrorLogHelper.class);
        mockStatic(DeviceInfoHelper.class);
        mockStatic(System.class);

        @SuppressWarnings("unchecked")
        AppCenterFuture<Boolean> future = (AppCenterFuture<Boolean>) mock(AppCenterFuture.class);
        when(AppCenter.isEnabled()).thenReturn(future);
        when(future.get()).thenReturn(true);
        when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(eq(CRASHES_ENABLED_KEY), anyBoolean());

        ManagedErrorLog errorLogMock = mock(ManagedErrorLog.class);
        when(ErrorLogHelper.getModelExceptionFromThrowable(any(Throwable.class))).thenCallRealMethod();
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mErrorStorageDirectory.getRoot());
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), any(), anyLong(), anyBoolean()))
                .thenReturn(errorLogMock);

        when(errorLogMock.getId()).thenReturn(UUID.randomUUID());
        when(errorLogMock.getException()).thenReturn(new com.microsoft.appcenter.crashes.ingestion.models.Exception());

        /* Mock handlers. */
        mockStatic(HandlerUtils.class);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        AppCenterHandler handler = mock(AppCenterHandler.class);
        doAnswer(runNow).when(handler).post(any(Runnable.class), any(Runnable.class));
        Crashes.getInstance().onStarting(handler);
        Crashes.getInstance().onStarted(mock(Context.class), mock(Channel.class), "mock", null, true);

        mDefaultExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(mDefaultExceptionHandler);
        mExceptionHandler = new UncaughtExceptionHandler();
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

        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), any(), anyLong(), anyBoolean());
    }

    @Test
    public void handleExceptionAndPassOnExplicitlySetDonNotIgnore() {
        mExceptionHandler.setIgnoreDefaultExceptionHandler(false);
        handleExceptionAndPassOn();
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

        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), any(), anyLong(), anyBoolean());
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

        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(jsonException));

        verify(mDefaultExceptionHandler).uncaughtException(thread, exception);
    }

    @Test
    public void testIOException() throws java.lang.Exception {
        mExceptionHandler.register();

        IOException ioException = new IOException("Fake IO exception");
        doThrow(ioException).when(FileManager.class);
        FileManager.write(any(File.class), any());

        final Thread thread = Thread.currentThread();
        final RuntimeException exception = new RuntimeException();
        mExceptionHandler.uncaughtException(thread, exception);

        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(ioException));

        verify(mDefaultExceptionHandler).uncaughtException(thread, exception);
    }
}
