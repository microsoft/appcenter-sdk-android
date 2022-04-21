/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import static android.util.Log.getStackTraceString;
import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HandlerUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@PrepareForTest({
        AppCenter.class,
        AppCenterLog.class,
        Crashes.class,
        ErrorLogHelper.class,
        FileManager.class,
        HandlerUtils.class,
        SharedPreferencesManager.class,
        WrapperSdkExceptionManager.class
})
public class WrapperSdkExceptionManagerTest {

    private static final String CRASHES_ENABLED_KEY = KEY_ENABLED + "_" + Crashes.getInstance().getServiceName();

    private static final String STACK_TRACE = "Sample stack trace";

    private Crashes mCrashes;

    @Rule
    public final PowerMockRule mRule = new PowerMockRule();

    @Rule
    public final TemporaryFolder mErrorStorageDirectory = new TemporaryFolder();

    @Before
    public void setUp() {
        Crashes.unsetInstance();
        mockStatic(AppCenter.class);
        mockStatic(AppCenterLog.class);
        mockStatic(ErrorLogHelper.class);
        mockStatic(FileManager.class);
        mockStatic(SharedPreferencesManager.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mErrorStorageDirectory.getRoot());
        ManagedErrorLog errorLogMock = mock(ManagedErrorLog.class);
        when(errorLogMock.getId()).thenReturn(UUID.randomUUID());
        when(errorLogMock.getException()).thenReturn(new com.microsoft.appcenter.crashes.ingestion.models.Exception());
        when(ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), any(), anyLong(), anyBoolean()))
                .thenReturn(errorLogMock);
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);

        @SuppressWarnings("unchecked")
        AppCenterFuture<Boolean> future = (AppCenterFuture<Boolean>) mock(AppCenterFuture.class);
        when(AppCenter.isEnabled()).thenReturn(future);
        when(future.get()).thenReturn(true);
        when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(true);

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
        doAnswer(runNow).when(handler).post(any(Runnable.class), any());
        mCrashes = Crashes.getInstance();
        mCrashes.onStarting(handler);
        mCrashes.onStarted(mock(Context.class), mock(Channel.class), "mock", null, true);
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void constructWrapperSdkExceptionManager() {
        new WrapperSdkExceptionManager();
    }

    @Test
    public void loadWrapperExceptionData() throws java.lang.Exception {
        File file = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(file);
        when(file.exists()).thenReturn(true);
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(UUID.randomUUID()));
        assertNull(WrapperSdkExceptionManager.loadWrapperExceptionData(null));
    }

    @Test
    public void deleteWrapperExceptionDataWithNullId() {

        /* Delete null does nothing. */
        WrapperSdkExceptionManager.deleteWrapperExceptionData(null);
        verifyStatic(FileManager.class, never());
        FileManager.delete(any(File.class));
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void deleteWrapperExceptionDataWithMissingId() {

        /* Delete with file not found does nothing. */
        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        verifyStatic(FileManager.class, never());
        FileManager.delete(any(File.class));
        verifyStatic(AppCenterLog.class, never());
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void deleteWrapperExceptionDataWithLoadingError() throws java.lang.Exception {

        /* Delete with file that cannot be loaded because invalid content should just log an error. */
        File file = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(file);
        when(file.exists()).thenReturn(true);
        WrapperSdkExceptionManager.deleteWrapperExceptionData(UUID.randomUUID());
        verifyStatic(FileManager.class);
        FileManager.delete(any(File.class));
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString());
    }

    @Test
    public void saveWrapperSdkCrash() throws JSONException, IOException {
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        mCrashes.setLogSerializer(logSerializer);
        String data = "d";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic(FileManager.class);
        FileManager.write(any(File.class), eq(data));

        /* We can't do it twice in the same process. */
        data = "e";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic(FileManager.class, never());
        FileManager.write(any(File.class), eq(data));
    }

    @Test
    @PrepareForTest(android.util.Log.class)
    public void saveWrapperSdkCrashWithJavaThrowable() throws JSONException, IOException {
        String mockData = "mock";
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn(mockData);
        mCrashes.setLogSerializer(logSerializer);
        String data = "d";
        Throwable throwable = new Throwable();
        mockStatic(android.util.Log.class);
        when(getStackTraceString(any(Throwable.class))).thenReturn(STACK_TRACE);
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), data);
        verifyStatic(FileManager.class);
        FileManager.write(any(File.class), eq(data));
        verifyStatic(FileManager.class);
        FileManager.write(any(File.class), eq(mockData));

        /* We can't do it twice in the same process. */
        data = "e";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), data);
        verifyStatic(FileManager.class, never());
        FileManager.write(any(File.class), eq(data));
        verifyStatic(FileManager.class);
        FileManager.write(any(File.class), eq(mockData));
    }

    @Test
    @PrepareForTest({android.util.Log.class})
    public void saveWrapperSdkCrashWithOnlyJavaThrowable() throws JSONException, IOException {
        String mockData = "mock";
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn(mockData);
        mCrashes.setLogSerializer(logSerializer);
        Throwable throwable = new Throwable();
        mockStatic(android.util.Log.class);
        when(getStackTraceString(any(Throwable.class))).thenReturn(STACK_TRACE);
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), null);
        verifyStatic(FileManager.class, never());
        FileManager.write(any(File.class), isNull());
        verifyStatic(FileManager.class);
        FileManager.write(any(File.class), eq(mockData));

        /* We can't do it twice in the same process. */
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), throwable, new Exception(), null);
        verifyStatic(FileManager.class);
        FileManager.write(any(File.class), eq(mockData));
    }

    @Test
    public void saveWrapperSdkCrashFailsToCreateThrowablePlaceholder() throws java.lang.Exception {
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        mCrashes.setLogSerializer(logSerializer);
        File throwableFile = mock(File.class);
        whenNew(File.class).withParameterTypes(String.class, String.class)
                .withArguments(anyString(), endsWith(ErrorLogHelper.THROWABLE_FILE_EXTENSION))
                .thenReturn(throwableFile);
        when(throwableFile.createNewFile()).thenReturn(false);
        String data = "d";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic(AppCenterLog.class, times(3));
        AppCenterLog.debug(anyString(), anyString());

        /* Second call is ignored. */
        data = "e";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);

        /* No more error. */
        verifyStatic(AppCenterLog.class, times(3));
        AppCenterLog.debug(anyString(), anyString());
    }

    @Test
    public void saveWrapperSdkCrashFailsWithJSONException() throws JSONException {
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenThrow(new JSONException("mock"));
        mCrashes.setLogSerializer(logSerializer);
        String data = "d";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), isA(JSONException.class));

        /* Second call is ignored. */
        data = "e";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);

        /* No more error. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), isA(JSONException.class));
    }

    @Test
    public void saveWrapperSdkCrashFailsWithIOException() throws IOException, JSONException {
        doThrow(new IOException()).when(FileManager.class);
        FileManager.write(any(File.class), anyString());
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        mCrashes.setLogSerializer(logSerializer);
        String data = "d";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), isA(IOException.class));

        /* Second call is ignored. */
        data = "e";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);

        /* No more error. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), isA(IOException.class));
    }

    @Test
    public void saveWrapperSdkCrashFailsWithIOExceptionAfterLog() throws IOException, JSONException {
        String data = "d";
        doThrow(new IOException()).when(FileManager.class);
        FileManager.write(any(File.class), eq(data));
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeLog(any(ManagedErrorLog.class))).thenReturn("mock");
        mCrashes.setLogSerializer(logSerializer);
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), isA(IOException.class));

        /* Second call is ignored. */
        data = "e";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);

        /* No more error. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), isA(IOException.class));
    }

    @Test
    public void saveWrapperExceptionWhenSDKDisabled() throws JSONException {
        when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(false);
        LogSerializer logSerializer = mock(LogSerializer.class);
        mCrashes.setLogSerializer(logSerializer);
        String data = "d";
        WrapperSdkExceptionManager.saveWrapperException(Thread.currentThread(), null, new Exception(), data);
        verify(logSerializer, never()).serializeLog(any(Log.class));
        verifyStatic(ErrorLogHelper.class, never());
        ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), any(Exception.class), any(), anyLong(), anyBoolean());
    }

    @Test
    @PrepareForTest(DeviceInfoHelper.class)
    public void handledErrorReportFailedToGetDeviceInfo() throws DeviceInfoHelper.DeviceInfoException {

        /* If device info fails. */
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenThrow(new DeviceInfoHelper.DeviceInfoException("mock", new java.lang.Exception()));

        /* When we build an handled error report. */
        String errorReportId = UUID.randomUUID().toString();

        /* Then error report is returned but without device info. */
        ErrorReport errorReport = WrapperSdkExceptionManager.buildHandledErrorReport(mock(Context.class), errorReportId);
        assertNotNull(errorReport);
        assertEquals(errorReportId, errorReport.getId());
        assertNotNull(errorReport.getAppErrorTime());
        assertNotNull(errorReport.getAppStartTime());
        assertNull(errorReport.getDevice());
    }
}
