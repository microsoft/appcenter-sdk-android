/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN;
import static android.util.Log.getStackTraceString;
import static com.microsoft.appcenter.Constants.WRAPPER_SDK_NAME_NDK;
import static com.microsoft.appcenter.Flags.CRITICAL;
import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.crashes.Crashes.MINIDUMP_FILE;
import static com.microsoft.appcenter.crashes.Crashes.PREF_KEY_MEMORY_RUNNING_LEVEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Looper;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.ingestion.models.HandledErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.crashes.ingestion.models.json.ErrorAttachmentLogFactory;
import com.microsoft.appcenter.crashes.ingestion.models.json.HandledErrorLogFactory;
import com.microsoft.appcenter.crashes.ingestion.models.json.ManagedErrorLogFactory;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.model.TestCrashException;
import com.microsoft.appcenter.crashes.utils.ErrorLogHelper;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.context.SessionContext;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@PrepareForTest({android.util.Log.class})
public class CrashesTest extends AbstractCrashesTest {

    private static final String STACK_TRACE = "type: message";

    private ManagedErrorLog mErrorLog;

    private static void assertErrorEquals(ManagedErrorLog errorLog, ErrorReport report) {
        assertNotNull(report);
        assertEquals(errorLog.getId().toString(), report.getId());
        assertEquals(errorLog.getErrorThreadName(), report.getThreadName());
        assertEquals(STACK_TRACE, report.getStackTrace());
        assertEquals(errorLog.getAppLaunchTimestamp(), report.getAppStartTime());
        assertEquals(errorLog.getTimestamp(), report.getAppErrorTime());
        assertEquals(errorLog.getDevice(), report.getDevice());
        assertNotNull(errorLog.getDevice());
    }

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        super.setUp();
        mockStatic(android.util.Log.class);
        when(getStackTraceString(any(Throwable.class))).thenReturn(STACK_TRACE);
        mErrorLog = ErrorLogHelper.createErrorLog(mock(Context.class), Thread.currentThread(), new RuntimeException(), new HashMap<>(), 0);
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
        UncaughtExceptionHandler mockHandler = mock(UncaughtExceptionHandler.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(dir);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(dir.listFiles()).thenReturn(new File[]{
                mock(File.class),
                mock(File.class)
        });
        crashes.setUncaughtExceptionHandler(mockHandler);
        when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(false);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);

        /* Test. */
        verifyStatic(SharedPreferencesManager.class, times(4));
        SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true);
        assertFalse(Crashes.isEnabled().get());
        assertEquals(crashes.getInitializeTimestamp(), -1);
        assertNull(crashes.getUncaughtExceptionHandler());
        verify(mockHandler).unregister();
    }

    @Test
    public void notInit() {
        Crashes.notifyUserConfirmation(Crashes.SEND);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void checkConfig() {
        Crashes instance = Crashes.getInstance();
        Map<String, LogFactory> factories = instance.getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(ManagedErrorLog.TYPE) instanceof ManagedErrorLogFactory);
        assertTrue(factories.remove(HandledErrorLog.TYPE) instanceof HandledErrorLogFactory);
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
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(dir.listFiles()).thenReturn(new File[]{file1, file2});

        /* Before start it's disabled. */
        assertFalse(Crashes.isEnabled().get());
        assertEquals(0, crashes.getInitializeTimestamp());

        /* Start. */
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mockChannel, "", null, true);
        verify(mockChannel).removeGroup(eq(crashes.getGroupName()));
        verify(mockChannel).addGroup(eq(crashes.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(), any(Channel.GroupListener.class));

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
        Crashes.trackError(EXCEPTION);
        verifyNoMoreInteractions(mockChannel);

        /* Enable back, testing double calls. */
        Crashes.setEnabled(true);
        assertTrue(Crashes.isEnabled().get());
        assertTrue(crashes.getInitializeTimestamp() > 0);
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionHandler);
        Crashes.setEnabled(true);
        assertTrue(Crashes.isEnabled().get());
        verify(mockChannel, times(2)).addGroup(eq(crashes.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(), any(Channel.GroupListener.class));
        Crashes.trackError(EXCEPTION);
        verify(mockChannel, times(1)).enqueue(isA(HandledErrorLog.class), eq(crashes.getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void failToListErrorStorageDirectoryOnDisable() {

        /* Setup mock. */
        File mockErrorFile = mock(File.class);
        when(mockErrorFile.listFiles()).thenReturn(null);
        when(mockErrorFile.isDirectory()).thenReturn(true);
        Crashes crashes = Crashes.getInstance();
        mockStatic(ErrorLogHelper.class);
        Context context = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        File dir = mock(File.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(dir);
        when(dir.listFiles()).thenReturn(null);
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[]{mockErrorFile});
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{});

        /* Start. */
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(context, mockChannel, "", null, true);
        verify(mockChannel).removeGroup(eq(crashes.getGroupName()));
        verify(mockChannel).addGroup(eq(crashes.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(), any(Channel.GroupListener.class));

        /* When we disable. */
        Crashes.setEnabled(false);
        assertFalse(Crashes.isEnabled().get());

        /* Verify we recovered file listing error. */
        verify(context).unregisterComponentCallbacks(notNull());
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
    public void queuePendingCrashesShouldProcess() throws JSONException {

        /* Setup mock. */
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        ErrorReport report = new ErrorReport();
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(report);
        when(FileManager.read(any(File.class))).thenReturn("");
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
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(mErrorLog);
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mockContext, mockChannel, "", null, true);

        /* Test. */
        verify(mockListener).shouldProcess(report);
        verify(mockListener).shouldAwaitUserConfirmation();
        verify(mockListener).getErrorAttachments(report);
        verify(mockChannel).enqueue(eq(mErrorLog), eq(crashes.getGroupName()), eq(CRITICAL));
        verify(mockChannel, times(errorAttachmentLogList.size() - skipAttachmentLogsCount))
                .enqueue(mockAttachment, crashes.getGroupName(), DEFAULTS);
    }

    @Test
    public void queuePendingCrashesShouldNotProcess() throws JSONException {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);

        ErrorReport report = new ErrorReport();

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(report);
        when(FileManager.read(any(File.class))).thenReturn("");

        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(report)).thenReturn(false);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(mErrorLog);

        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mockContext, mockChannel, "", null, true);

        verify(mockListener).shouldProcess(report);
        verify(mockListener, never()).shouldAwaitUserConfirmation();

        verify(mockListener, never()).getErrorAttachments(report);
        verify(mockChannel, never()).enqueue(any(Log.class), eq(crashes.getGroupName()), anyInt());
    }

    @Test
    public void queuePendingCrashesAlwaysSend() throws JSONException {
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
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(report);
        when(FileManager.read(any(File.class))).thenReturn("");
        when(SharedPreferencesManager.getBoolean(eq(Crashes.PREF_KEY_ALWAYS_SEND), anyBoolean())).thenReturn(true);

        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(report)).thenReturn(true);

        when(mockListener.shouldProcess(report)).thenReturn(true);
        when(mockListener.shouldAwaitUserConfirmation()).thenReturn(false);

        when(mockListener.getErrorAttachments(report)).thenReturn(errorAttachmentLogList);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(mErrorLog);

        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mockContext, mockChannel, "", null, true);

        verify(mockListener).shouldProcess(report);
        verify(mockListener, never()).shouldAwaitUserConfirmation();

        verify(mockListener).getErrorAttachments(report);
        verify(mockChannel).enqueue(eq(mErrorLog), eq(crashes.getGroupName()), eq(CRITICAL));

        verify(mockChannel, times(errorAttachmentLogList.size())).enqueue(mockAttachment, crashes.getGroupName(), DEFAULTS);
    }

    @Test
    public void processPendingErrorsCorrupted() throws JSONException {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn("");

        Crashes crashes = Crashes.getInstance();

        LogSerializer logSerializer = mock(LogSerializer.class);
        com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = new com.microsoft.appcenter.crashes.ingestion.models.Exception();
        mockException.setType("type");
        mockException.setMessage("message");
        ManagedErrorLog managedErrorLog = mock(ManagedErrorLog.class);
        when(managedErrorLog.getException()).thenReturn(mockException);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(managedErrorLog);
        crashes.setLogSerializer(logSerializer);

        CrashesListener listener = mock(CrashesListener.class);
        crashes.setInstanceListener(listener);

        Channel channel = mock(Channel.class);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), channel, "", null, true);
        verifyNoInteractions(listener);
        verify(channel, never()).enqueue(any(Log.class), anyString(), anyInt());
    }

    @Test
    public void noQueueingWhenDisabled() {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mTemporaryFolder.getRoot());
        when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(false);
        Channel channel = mock(Channel.class);
        Crashes crashes = Crashes.getInstance();
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel, never()).enqueue(any(Log.class), anyString(), anyInt());
    }

    @Test
    public void noQueueNullLog() throws JSONException {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(null);
        crashes.setLogSerializer(logSerializer);

        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mockContext, mockChannel, "", null, true);

        verify(mockChannel, never()).enqueue(any(Log.class), anyString(), anyInt());
    }

    @Test
    public void printErrorOnJSONException() throws JSONException {
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        JSONException jsonException = new JSONException("Fake JSON exception");

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn("");
        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);

        when(logSerializer.deserializeLog(anyString(), any())).thenThrow(jsonException);
        crashes.setLogSerializer(logSerializer);

        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mockContext, mockChannel, "", null, true);

        verify(mockChannel, never()).enqueue(any(Log.class), anyString(), anyInt());

        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(jsonException));
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
    public void getChannelListener() throws JSONException {

        /* Mock and set exception data. */
        com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = mock(com.microsoft.appcenter.crashes.ingestion.models.Exception.class);
        when(mockException.getType()).thenReturn("type");
        when(mockException.getMessage()).thenReturn("message");
        mErrorLog.setException(mockException);

        ErrorReport errorReport = ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE);
        mockStatic(ErrorLogHelper.class);
        File errorLogFile = mock(File.class);
        when(errorLogFile.length()).thenReturn(1L);
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(errorLogFile);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE)).thenReturn(errorReport);
        when(FileManager.read(any(File.class))).thenReturn("");
        when(FileManager.read(any(File.class))).thenReturn(STACK_TRACE);

        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(mErrorLog);
        CrashesListener crashesListener = mock(CrashesListener.class);
        when(crashesListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        Crashes.setListener(crashesListener);
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);

        /* The error report was created and cached but device is null here. */
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE);
        assertNull(errorReport.getDevice());

        /* The channel sets a device. */
        mErrorLog.setDevice(mock(Device.class));

        ArgumentCaptor<ErrorReport> errorReportCaptor = ArgumentCaptor.forClass(ErrorReport.class);
        Channel.GroupListener channelListener = crashes.getChannelListener();

        /* Simulate onBeforeSending event. */
        channelListener.onBeforeSending(mErrorLog);
        verify(crashesListener).onBeforeSending(errorReportCaptor.capture());
        assertErrorEquals(mErrorLog, errorReportCaptor.getValue());

        /* Simulate onSuccess event. */
        channelListener.onSuccess(mErrorLog);
        verify(crashesListener).onSendingSucceeded(errorReportCaptor.capture());
        assertErrorEquals(mErrorLog, errorReportCaptor.getValue());

        /* No more error reports should be produced at the point. */
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE);

        /* Simulate onFailure event. */
        channelListener.onFailure(mErrorLog, EXCEPTION);
        verify(crashesListener).onSendingFailed(errorReportCaptor.capture(), eq(EXCEPTION));
        assertErrorEquals(mErrorLog, errorReportCaptor.getValue());

        /* onSuccess and onFailure invalidate the cache, so one more call is expected. */
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE);
    }

    @Test
    public void getChannelListenerErrors() {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn(null);

        CrashesListener mockListener = mock(CrashesListener.class);
        Crashes crashes = Crashes.getInstance();
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);

        Channel.GroupListener listener = Crashes.getInstance().getChannelListener();

        listener.onBeforeSending(mErrorLog);
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(Crashes.LOG_TAG), anyString());
        Mockito.verifyNoMoreInteractions(mockListener);

        listener.onSuccess(mock(Log.class));
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(Crashes.LOG_TAG), contains(Log.class.getName()));
        Mockito.verifyNoMoreInteractions(mockListener);
    }

    @Test
    public void handleUserConfirmationDoNotSend() throws JSONException {

        /* Prepare data. Mock classes. */
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(new ErrorReport());
        File pendingFolder = mock(File.class);
        when(ErrorLogHelper.getPendingMinidumpDirectory()).thenReturn(pendingFolder);
        when(FileManager.read(any(File.class))).thenReturn("");
        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(mockListener.shouldAwaitUserConfirmation()).thenReturn(true);

        /* Prepare data. Set values. */
        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(mErrorLog);
        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);

        /* Verify. */
        Crashes.notifyUserConfirmation(Crashes.DONT_SEND);
        verify(mockListener, never()).getErrorAttachments(any(ErrorReport.class));
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.cleanPendingMinidumps();
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.removeStoredErrorLogFile(mErrorLog.getId());
        verifyStatic(ErrorLogHelper.class, never());
        ErrorLogHelper.removeLostThrowableFiles();
    }

    @Test
    public void handleUserConfirmationAlwaysSend() throws JSONException {

        /* Simulate the method is called from Worker Thread. */
        mockStatic(Looper.class);
        when(Looper.myLooper()).thenReturn(mock(Looper.class));
        when(Looper.getMainLooper()).thenReturn(null);

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn(null);

        CrashesListener mockListener = mock(CrashesListener.class);
        when(mockListener.shouldProcess(any(ErrorReport.class))).thenReturn(true);

        Crashes crashes = Crashes.getInstance();
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(mErrorLog);

        crashes.setLogSerializer(logSerializer);
        crashes.setInstanceListener(mockListener);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);

        Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);

        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(Crashes.PREF_KEY_ALWAYS_SEND, true);
    }

    @Test
    public void buildErrorReport() {

        /* Mock and set exception data. */
        com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = mock(com.microsoft.appcenter.crashes.ingestion.models.Exception.class);
        when(mockException.getType()).thenReturn("type");
        when(mockException.getMessage()).thenReturn("message");
        mErrorLog.setException(mockException);
        mErrorLog.setDevice(mock(Device.class));
        ErrorReport errorReport = ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE);

        mockStatic(ErrorLogHelper.class);
        File throwableFile = mock(File.class);
        when(throwableFile.length()).thenReturn(1L);
        when(ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE)).thenReturn(errorReport);
        when(FileManager.read(any(File.class))).thenReturn(STACK_TRACE);

        Crashes crashes = Crashes.getInstance();
        ErrorReport report = crashes.buildErrorReport(mErrorLog);
        assertErrorEquals(mErrorLog, report);
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE);

        /* Verify the caching. */
        assertEquals(report, crashes.buildErrorReport(mErrorLog));
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.getErrorReportFromErrorLog(mErrorLog, STACK_TRACE);

        mErrorLog.setId(UUID.randomUUID());
        report = crashes.buildErrorReport(mErrorLog);
        assertNotNull(report);
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
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        assertNull(Crashes.getLastSessionCrashReport().get());
        Crashes.getInstance().onStarted(mock(Context.class), mock(Channel.class), "", null, true);
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());
        verifyStatic(AppCenterLog.class, never());
        AppCenterLog.debug(anyString(), anyString());
    }

    @Test
    public void crashInLastSession() throws JSONException, IOException {

        /* Mock and set exception data. */
        com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = mock(com.microsoft.appcenter.crashes.ingestion.models.Exception.class);
        when(mockException.getType()).thenReturn("type");
        when(mockException.getMessage()).thenReturn("message");

        final ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUID.randomUUID());
        errorLog.setErrorThreadName(Thread.currentThread().getName());
        errorLog.setException(mockException);
        Date logTimestamp = new Date(10);
        errorLog.setTimestamp(logTimestamp);

        Date appLaunchTimestamp = new Date(100L);
        errorLog.setAppLaunchTimestamp(appLaunchTimestamp);
        errorLog.setDevice(mock(Device.class));

        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(errorLog);

        final ErrorReport errorReport = ErrorLogHelper.getErrorReportFromErrorLog(errorLog, STACK_TRACE);

        mockStatic(ErrorLogHelper.class);
        File lastErrorLogFile = mTemporaryFolder.newFile("last-error-log.json");
        new FileWriter(lastErrorLogFile).append("fake_data").close();
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(lastErrorLogFile);
        when(ErrorLogHelper.getErrorReportFromErrorLog(errorLog, STACK_TRACE)).thenReturn(errorReport);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{lastErrorLogFile});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn("fake_data").thenReturn(STACK_TRACE);

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
        AppCenterConsumer<ErrorReport> beforeCallback = (AppCenterConsumer<ErrorReport>) mock(AppCenterConsumer.class);
        Crashes.getLastSessionCrashReport().thenAccept(beforeCallback);
        verify(beforeCallback).accept(null);

        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);
        assertTrue(Crashes.isEnabled().get());
        assertTrue(Crashes.hasCrashedInLastSession().get());

        /* Test with 2 callbacks and check result is the same for both callbacks. */
        @SuppressWarnings("unchecked")
        AppCenterConsumer<ErrorReport> afterCallback = (AppCenterConsumer<ErrorReport>) mock(AppCenterConsumer.class);
        AppCenterFuture<ErrorReport> future = Crashes.getLastSessionCrashReport();
        future.thenAccept(afterCallback);
        future.thenAccept(afterCallback);
        ArgumentCaptor<ErrorReport> errorReportCaptor = ArgumentCaptor.forClass(ErrorReport.class);
        verify(afterCallback, times(2)).accept(errorReportCaptor.capture());
        assertEquals(errorReportCaptor.getAllValues().get(0), errorReportCaptor.getAllValues().get(1));
        ErrorReport result = errorReportCaptor.getValue();
        assertNotNull(result);
        assertEquals(errorLog.getId().toString(), result.getId());
        assertEquals(errorLog.getErrorThreadName(), result.getThreadName());
        assertEquals(appLaunchTimestamp, result.getAppStartTime());
        assertEquals(logTimestamp, result.getAppErrorTime());
        assertNotNull(result.getDevice());
        assertEquals(STACK_TRACE, result.getStackTrace());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void getThrowableDeprecated() {
        ErrorReport report = new ErrorReport();
        assertNull(report.getThrowable());
    }

    @Test
    public void noCrashInLastSessionWhenDisabled() {

        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mTemporaryFolder.getRoot());

        Crashes.setEnabled(false);

        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());

        verifyStatic(ErrorLogHelper.class, never());
        ErrorLogHelper.getLastErrorLogFile();
    }

    @Test
    public void failToDeserializeLastSessionCrashReport() throws JSONException, IOException {
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(mock(ManagedErrorLog.class));

        mockStatic(ErrorLogHelper.class);
        File lastErrorLogFile = mTemporaryFolder.newFile("last-error-log.json");
        new FileWriter(lastErrorLogFile).append("fake_data").close();
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(lastErrorLogFile);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{lastErrorLogFile});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn("fake_data");

        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);

        assertFalse(Crashes.hasCrashedInLastSession().get());

        JSONException jsonException = new JSONException("Fake JSON exception");
        when(logSerializer.deserializeLog(anyString(), any())).thenThrow(jsonException);

        /*
         * Last session error is only fetched upon initialization: enabled and channel ready.
         * Here the service is disabled by default until started, we are waiting channel to be ready, simulate that.
         */
        assertFalse(Crashes.isEnabled().get());
        assertNull(Crashes.getLastSessionCrashReport().get());
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);
        assertTrue(Crashes.isEnabled().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());

        /*
         * De-serializing fails twice: processing the log from last time as part of the bulk processing.
         * And loading that same file for exposing it in getLastErrorReport.
         */
        verifyStatic(AppCenterLog.class, times(2));
        AppCenterLog.error(eq(Crashes.LOG_TAG), anyString(), eq(jsonException));
        verifyStatic(ErrorLogHelper.class);
        ErrorLogHelper.removeLostThrowableFiles();
    }

    @Test
    public void crashInLastSessionCorrupted() throws IOException {
        mockStatic(ErrorLogHelper.class);
        File file = mTemporaryFolder.newFile("last-error-log.json");
        new FileWriter(file).append("fake_data").close();
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{file});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(file);
        Crashes.getInstance().onStarted(mock(Context.class), mock(Channel.class), "", null, true);
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());
    }

    @Test
    public void getLastSessionCrashReportWithMultipleListenersAndResultIsNullBeforeInit() {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(null);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);

        @SuppressWarnings("unchecked")
        AppCenterConsumer<ErrorReport> callback = (AppCenterConsumer<ErrorReport>) mock(AppCenterConsumer.class);

        /* Call twice for multiple callbacks before initialize. */
        Crashes.getLastSessionCrashReport().thenAccept(callback);
        Crashes.getLastSessionCrashReport().thenAccept(callback);
        Crashes.getInstance().onStarted(mock(Context.class), mock(Channel.class), "", null, true);
        assertFalse(Crashes.hasCrashedInLastSession().get());
        verify(callback, times(2)).accept(null);
    }

    @Test
    public void discardHugeErrorAttachments() throws JSONException {

        /* Prepare a big (too big) attachment and a small one. */
        ArrayList<ErrorAttachmentLog> errorAttachmentLogs = new ArrayList<>(2);
        ErrorAttachmentLog binaryAttachment = ErrorAttachmentLog.attachmentWithBinary(new byte[7 * 1024 * 1024 + 1], "earth.png", "image/png");
        errorAttachmentLogs.add(binaryAttachment);
        ErrorAttachmentLog textAttachment = ErrorAttachmentLog.attachmentWithText("hello", "log.txt");
        errorAttachmentLogs.add(textAttachment);

        /* Set up callbacks. */
        CrashesListener listener = mock(CrashesListener.class);
        when(listener.shouldProcess(any(ErrorReport.class))).thenReturn(true);
        when(listener.getErrorAttachments(any(ErrorReport.class))).thenReturn(errorAttachmentLogs);

        /* Mock a crash log to process. */
        com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = new com.microsoft.appcenter.crashes.ingestion.models.Exception();
        mockException.setType("type");
        mockException.setMessage("message");
        ManagedErrorLog log = mock(ManagedErrorLog.class);
        when(log.getId()).thenReturn(UUID.randomUUID());
        when(log.getException()).thenReturn(mockException);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenReturn(log);
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(new ErrorReport());
        when(FileManager.read(any(File.class))).thenReturn("");

        /* Mock starting crashes so that attachments are processed. */
        Crashes crashes = Crashes.getInstance();
        crashes.setInstanceListener(listener);
        crashes.setLogSerializer(logSerializer);
        crashes.onStarting(mAppCenterHandler);
        Channel channel = mock(Channel.class);
        crashes.onStarted(mock(Context.class), channel, "", null, true);

        /* Check we send only the text attachment as the binary is too big. */
        verify(channel).enqueue(textAttachment, crashes.getGroupName(), NORMAL);
        verify(channel, never()).enqueue(eq(binaryAttachment), anyString(), anyInt());
    }

    @Test
    public void manualProcessing() throws Exception {

        /* Setup mock for a crash in disk. */
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        ErrorReport report1 = new ErrorReport();
        report1.setId(UUID.randomUUID().toString());
        ErrorReport report2 = new ErrorReport();
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class), mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(report1).thenReturn(report2);
        when(FileManager.read(any(File.class))).thenReturn("");
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenAnswer(new Answer<ManagedErrorLog>() {

            @Override
            public ManagedErrorLog answer(InvocationOnMock invocation) {
                com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = new com.microsoft.appcenter.crashes.ingestion.models.Exception();
                mockException.setType("type");
                mockException.setMessage("message");
                ManagedErrorLog log = mock(ManagedErrorLog.class);
                when(log.getId()).thenReturn(UUID.randomUUID());
                when(log.getException()).thenReturn(mockException);
                return log;
            }
        });
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);

        /* Create listener for user confirmation. */
        CrashesListener listener = mock(CrashesListener.class);
        Crashes.setListener(listener);

        /* Set manual processing. */
        WrapperSdkExceptionManager.setAutomaticProcessing(false);

        /* Start crashes. */
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mockContext, mockChannel, "", null, true);

        /* No log queued. */
        verify(mockChannel, never()).enqueue(any(Log.class), eq(crashes.getGroupName()), anyInt());

        /* Get crash reports. */
        Collection<ErrorReport> reports = WrapperSdkExceptionManager.getUnprocessedErrorReports().get();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        Iterator<ErrorReport> iterator = reports.iterator();
        assertEquals(report1, iterator.next());
        assertEquals(report2, iterator.next());

        /* Listener not called yet on anything on manual processing. */
        verifyNoInteractions(listener);

        /* Send only the first. */
        assertFalse(WrapperSdkExceptionManager.sendCrashReportsOrAwaitUserConfirmation(Collections.singletonList(report1.getId())).get());

        /* We used manual process function, listener not called. */
        verifyNoInteractions(listener);

        /* No log sent until manual user confirmation in that mode (we are not in always send). */
        verify(mockChannel, never()).enqueue(any(ManagedErrorLog.class), eq(crashes.getGroupName()), anyInt());

        /* Confirm with always send. */
        Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putBoolean(Crashes.PREF_KEY_ALWAYS_SEND, true);
        when(SharedPreferencesManager.getBoolean(eq(Crashes.PREF_KEY_ALWAYS_SEND), anyBoolean())).thenReturn(true);

        /* 1 log sent. Other one is filtered. */
        verify(mockChannel).enqueue(any(ManagedErrorLog.class), eq(crashes.getGroupName()), eq(CRITICAL));

        /* We can send attachments via wrapper instead of using listener (both work but irrelevant to test with listener). */
        ErrorAttachmentLog mockAttachment = mock(ErrorAttachmentLog.class);
        when(mockAttachment.getId()).thenReturn(UUID.randomUUID());
        when(mockAttachment.getData()).thenReturn(new byte[0]);
        when(mockAttachment.isValid()).thenReturn(true);
        WrapperSdkExceptionManager.sendErrorAttachments(report1.getId(), Collections.singletonList(mockAttachment));
        verify(mockChannel).enqueue(eq(mockAttachment), eq(crashes.getGroupName()), eq(DEFAULTS));

        /* Send attachment with invalid UUID format for report identifier. */
        mockAttachment = mock(ErrorAttachmentLog.class);
        when(mockAttachment.getId()).thenReturn(UUID.randomUUID());
        when(mockAttachment.getData()).thenReturn(new byte[0]);
        when(mockAttachment.isValid()).thenReturn(true);
        WrapperSdkExceptionManager.sendErrorAttachments("not-a-uuid", Collections.singletonList(mockAttachment));
        verify(mockChannel, never()).enqueue(eq(mockAttachment), eq(crashes.getGroupName()), anyInt());

        /* We used manual process function, listener not called and our mock channel does not send events. */
        verifyNoInteractions(listener);

        /* Reset instance to test another tine with always send. */
        Crashes.unsetInstance();
        crashes = Crashes.getInstance();
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(report1).thenReturn(report2);
        WrapperSdkExceptionManager.setAutomaticProcessing(false);
        crashes.setLogSerializer(logSerializer);
        crashes.onStarting(mAppCenterHandler);
        mockChannel = mock(Channel.class);
        crashes.onStarted(mockContext, mockChannel, "", null, true);
        assertTrue(Crashes.isEnabled().get());
        verify(mockChannel, never()).enqueue(any(ManagedErrorLog.class), eq(crashes.getGroupName()), anyInt());

        /* Get crash reports, check always sent was returned and sent without confirmation. */
        assertTrue(WrapperSdkExceptionManager.sendCrashReportsOrAwaitUserConfirmation(Collections.singletonList(report2.getId())).get());
        verify(mockChannel).enqueue(any(ManagedErrorLog.class), eq(crashes.getGroupName()), eq(CRITICAL));
    }

    @Test
    public void manuallyReportNullList() throws Exception {

        /* Setup mock for a crash in disk. */
        Context mockContext = mock(Context.class);
        Channel mockChannel = mock(Channel.class);
        ErrorReport report1 = new ErrorReport();
        report1.setId(UUID.randomUUID().toString());
        ErrorReport report2 = new ErrorReport();
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class), mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(report1).thenReturn(report2);
        when(FileManager.read(any(File.class))).thenReturn("");
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenAnswer(new Answer<ManagedErrorLog>() {

            @Override
            public ManagedErrorLog answer(InvocationOnMock invocation) {
                com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = new com.microsoft.appcenter.crashes.ingestion.models.Exception();
                mockException.setType("type");
                mockException.setMessage("message");
                ManagedErrorLog log = mock(ManagedErrorLog.class);
                when(log.getId()).thenReturn(UUID.randomUUID());
                when(log.getException()).thenReturn(mockException);
                return log;
            }
        });
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);

        /* Set manual processing. */
        WrapperSdkExceptionManager.setAutomaticProcessing(false);

        /* Start crashes. */
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mockContext, mockChannel, "", null, true);

        /* No log queued. */
        verify(mockChannel, never()).enqueue(any(Log.class), eq(crashes.getGroupName()), anyInt());

        /* Get crash reports. */
        Collection<ErrorReport> reports = WrapperSdkExceptionManager.getUnprocessedErrorReports().get();
        assertNotNull(reports);
        assertEquals(2, reports.size());
        Iterator<ErrorReport> iterator = reports.iterator();
        assertEquals(report1, iterator.next());
        assertEquals(report2, iterator.next());

        /* Send nothing using null. */
        assertFalse(WrapperSdkExceptionManager.sendCrashReportsOrAwaitUserConfirmation(null).get());

        /* No log sent. */
        verify(mockChannel, never()).enqueue(any(ManagedErrorLog.class), eq(crashes.getGroupName()), anyInt());
    }

    private ManagedErrorLog testNativeCrashLog(long appStartTime, long crashTime, boolean correlateSession, boolean hasDeviceInfo, boolean hasUserId) throws Exception {

        /* Create minidump sub-folder. */
        File minidumpSubfolder = mTemporaryFolder.newFolder("mockFolder");
        String mockUserId = "user-id";

        /* Create a file for a crash in disk. */
        File minidumpFile = new File(minidumpSubfolder, "mockFile.dmp");
        assertTrue(minidumpFile.createNewFile());
        assertTrue(minidumpFile.setLastModified(crashTime));

        /* Create an additional file in a folder to be filtered later. */
        File otherFile = new File(minidumpSubfolder, "otherFile.txt");
        long fakeCrashTime = new Date().getTime();
        assertTrue(otherFile.createNewFile());
        assertTrue(otherFile.setLastModified(fakeCrashTime));

        /* Mock session context. */
        mockStatic(SessionContext.class);
        SessionContext sessionContext = mock(SessionContext.class);
        when(SessionContext.getInstance()).thenReturn(sessionContext);
        if (correlateSession) {
            SessionContext.SessionInfo sessionInfo = mock(SessionContext.SessionInfo.class);
            when(sessionContext.getSessionAt(crashTime)).thenReturn(sessionInfo);
            when(sessionInfo.getAppLaunchTimestamp()).thenReturn(appStartTime);
        }

        /* Mock device info and ErrorLogHelper. */
        mockStatic(ErrorLogHelper.class);
        mockStatic(DeviceInfoHelper.class);
        Device device = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mock(Device.class));
        when(ErrorLogHelper.getStoredDeviceInfo(any(File.class))).thenReturn(hasDeviceInfo ? device : null);
        when(ErrorLogHelper.getStoredUserInfo(any(File.class))).thenReturn(hasUserId ? mockUserId: null);
        ErrorReport report = new ErrorReport();
        File errorLogFile = mock(File.class);
        when(errorLogFile.length()).thenReturn(1L);
        when(ErrorLogHelper.getLastErrorLogFile()).thenReturn(errorLogFile);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[]{minidumpSubfolder});
        File pendingDir = mock(File.class);
        Whitebox.setInternalState(pendingDir, "path", "");
        when(ErrorLogHelper.getPendingMinidumpDirectory()).thenReturn(pendingDir);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(report);
        when(ErrorLogHelper.parseLogFolderUuid(any(File.class))).thenReturn(UUID.randomUUID());
        when(FileManager.read(any(File.class))).thenReturn("");
        LogSerializer logSerializer = mock(LogSerializer.class);
        ArgumentCaptor<Log> log = ArgumentCaptor.forClass(Log.class);
        when(logSerializer.serializeLog(log.capture())).thenReturn("{}");
        when(logSerializer.deserializeLog(anyString(), any())).thenAnswer(new Answer<ManagedErrorLog>() {

            @Override
            public ManagedErrorLog answer(InvocationOnMock invocation) {
                com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = new com.microsoft.appcenter.crashes.ingestion.models.Exception();
                mockException.setType(MINIDUMP_FILE);
                mockException.setMessage("message");
                ManagedErrorLog log = mock(ManagedErrorLog.class);
                when(log.getId()).thenReturn(UUID.randomUUID());
                when(log.getException()).thenReturn(mockException);
                return log;
            }
        });

        /* Start crashes. */
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);

        /* Verify timestamps on the crash log. */
        assertTrue(Crashes.hasCrashedInLastSession().get());
        assertTrue(log.getValue() instanceof ManagedErrorLog);
        assertEquals(1, log.getAllValues().size());
        assertNotEquals(new Date(fakeCrashTime), log.getValue().getTimestamp());
        return (ManagedErrorLog) log.getValue();
    }

    @Test
    @PrepareForTest({SessionContext.class, DeviceInfoHelper.class})
    public void minidumpDeviceInfoNull() throws Exception {
        long appStartTime = 95000L;
        long crashTime = 126000L;
        ManagedErrorLog crashLog = testNativeCrashLog(appStartTime, crashTime, true, false, false);
        assertEquals(new Date(crashTime), crashLog.getTimestamp());
        assertEquals(new Date(appStartTime), crashLog.getAppLaunchTimestamp());
    }

    @Test
    @PrepareForTest({SessionContext.class, DeviceInfoHelper.class})
    public void minidumpAppLaunchTimestampFromSessionContext() throws Exception {
        long appStartTime = 99000L;
        long crashTime = 123000L;
        ManagedErrorLog crashLog = testNativeCrashLog(appStartTime, crashTime, true, true, true);
        assertEquals(new Date(crashTime), crashLog.getTimestamp());
        assertEquals(new Date(appStartTime), crashLog.getAppLaunchTimestamp());
    }

    @Test
    @PrepareForTest({SessionContext.class, DeviceInfoHelper.class})
    public void minidumpAppLaunchTimestampFromSessionContextInFuture() throws Exception {
        long appStartTime = 101000L;
        long crashTime = 100000L;
        ManagedErrorLog crashLog = testNativeCrashLog(appStartTime, crashTime, true, true, true);

        /* Verify we fall back to crash time for app start time. */
        assertEquals(new Date(crashTime), crashLog.getTimestamp());
        assertEquals(new Date(crashTime), crashLog.getAppLaunchTimestamp());
    }

    @Test
    @PrepareForTest({SessionContext.class, DeviceInfoHelper.class})
    public void minidumpAppLaunchTimestampFromSessionContextNotFound() throws Exception {
        long appStartTime = 99000L;
        long crashTime = 123000L;
        ManagedErrorLog crashLog = testNativeCrashLog(appStartTime, crashTime, false, true, true);

        /* Verify we fall back to crash time for app start time. */
        assertEquals(new Date(crashTime), crashLog.getTimestamp());
        assertEquals(new Date(crashTime), crashLog.getAppLaunchTimestamp());
    }

    @Test
    @PrepareForTest({SessionContext.class, DeviceInfoHelper.class})
    public void minidumpDeviceUserIdNull() throws Exception {
        long appStartTime = 95000L;
        long crashTime = 126000L;
        ManagedErrorLog crashLog = testNativeCrashLog(appStartTime, crashTime, true, false, false);
        assertNull(crashLog.getUserId());
    }

    @Test
    @PrepareForTest({SessionContext.class, DeviceInfoHelper.class})
    public void minidumpDeviceUserIdNotNull() throws Exception {
        long appStartTime = 95000L;
        long crashTime = 126000L;
        ManagedErrorLog crashLog = testNativeCrashLog(appStartTime, crashTime, true, false, true);
        assertNotNull(crashLog.getUserId());
    }

    @Test
    public void minidumpFilePathNull() throws Exception {

        /* Set up mock for the crash. */
        final com.microsoft.appcenter.crashes.ingestion.models.Exception exception = mock(com.microsoft.appcenter.crashes.ingestion.models.Exception.class);
        DefaultLogSerializer defaultLogSerializer = mock(DefaultLogSerializer.class);
        mockStatic(ErrorLogHelper.class);
        mockStatic(ErrorAttachmentLog.class);
        ErrorReport errorReport = new ErrorReport();
        Device device = new Device();
        device.setWrapperSdkName(WRAPPER_SDK_NAME_NDK);
        errorReport.setDevice(device);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(errorReport);
        whenNew(DefaultLogSerializer.class).withAnyArguments().thenReturn(defaultLogSerializer);
        whenNew(com.microsoft.appcenter.crashes.ingestion.models.Exception.class).withAnyArguments().thenReturn(exception);
        when(exception.getMinidumpFilePath()).thenReturn(null);
        when(exception.getType()).thenReturn(MINIDUMP_FILE);
        when(exception.getMessage()).thenReturn("Error message");
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class), mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn("");
        String jsonCrash = "{}";
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenAnswer(new Answer<ManagedErrorLog>() {

            @Override
            public ManagedErrorLog answer(InvocationOnMock invocation) {
                ManagedErrorLog log = mock(ManagedErrorLog.class);
                when(log.getId()).thenReturn(UUID.randomUUID());
                when(log.getException()).thenReturn(exception);
                return log;
            }
        });
        when(logSerializer.serializeLog(any(Log.class))).thenReturn(jsonCrash);
        when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(true);
        ErrorAttachmentLog errorAttachmentLog = mock(ErrorAttachmentLog.class);
        whenNew(ErrorAttachmentLog.class).withAnyArguments().thenReturn(errorAttachmentLog);

        /* Start crashes. */
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "secret-app-mock", null, true);

        /*
         * Verify that attachmentWithBinary doesn't get called if minidump is missing.
         * This scenario used to crash before, so if the test succeeds that also tests the crash is fixed.
         */
        verifyStatic(ErrorAttachmentLog.class, never());
        ErrorAttachmentLog.attachmentWithBinary(new byte[]{anyByte()}, anyString(), anyString());
    }

    @Test
    public void minidumpStoredWithOldSDK() throws Exception {

        /* Set up mock for the crash. */
        final com.microsoft.appcenter.crashes.ingestion.models.Exception exception = mock(com.microsoft.appcenter.crashes.ingestion.models.Exception.class);
        DefaultLogSerializer defaultLogSerializer = mock(DefaultLogSerializer.class);
        mockStatic(ErrorLogHelper.class);
        mockStatic(ErrorAttachmentLog.class);
        ErrorReport errorReport = new ErrorReport();
        Device device = new Device();
        device.setWrapperSdkName(WRAPPER_SDK_NAME_NDK);
        errorReport.setDevice(device);
        when(ErrorLogHelper.getErrorReportFromErrorLog(any(ManagedErrorLog.class), anyString())).thenReturn(errorReport);
        whenNew(DefaultLogSerializer.class).withAnyArguments().thenReturn(defaultLogSerializer);
        whenNew(com.microsoft.appcenter.crashes.ingestion.models.Exception.class).withAnyArguments().thenReturn(exception);
        when(exception.getStackTrace()).thenReturn("some minidump");
        when(exception.getType()).thenReturn(MINIDUMP_FILE);
        when(exception.getMessage()).thenReturn("message");

        /* This mocks we already processed minidump to convert to pending regular crash report as that would be the case if migrating data from older SDK. */
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[]{mock(File.class)});
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn("");
        String jsonCrash = "{}";
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.deserializeLog(anyString(), any())).thenAnswer(new Answer<ManagedErrorLog>() {

            @Override
            public ManagedErrorLog answer(InvocationOnMock invocation) {
                ManagedErrorLog log = mock(ManagedErrorLog.class);
                when(log.getId()).thenReturn(UUID.randomUUID());
                when(log.getException()).thenReturn(exception);
                when(log.getTimestamp()).thenReturn(new Date());
                return log;
            }
        });
        when(logSerializer.serializeLog(any(Log.class))).thenReturn(jsonCrash);
        when(SharedPreferencesManager.getBoolean(CRASHES_ENABLED_KEY, true)).thenReturn(true);

        ErrorAttachmentLog errorAttachmentLog = mock(ErrorAttachmentLog.class);
        when(ErrorAttachmentLog.attachmentWithBinary(any(), anyString(), anyString())).thenReturn(mock(ErrorAttachmentLog.class));
        whenNew(ErrorAttachmentLog.class).withAnyArguments().thenReturn(errorAttachmentLog);

        /* Start crashes. */
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "secret-app-mock", null, true);

        /* Verify that attachmentWithBinary does get sent. */
        verifyStatic(ErrorAttachmentLog.class);
        ErrorAttachmentLog.attachmentWithBinary(any(), anyString(), anyString());

        /* Verify temporary field erased. */
        verify(exception).setStackTrace(isNull());
    }

    @Test
    public void stackOverflowOnSavingThrowable() throws Exception {

        /* Mock error log utils. */
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getModelExceptionFromThrowable(any(Throwable.class))).thenCallRealMethod();
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mock(File.class));
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.createErrorLog(any(Context.class), any(Thread.class), notNull(), anyMap(), anyLong(), anyBoolean())).thenReturn(mErrorLog);
        File throwableFile = mock(File.class);
        whenNew(File.class).withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), endsWith(ErrorLogHelper.THROWABLE_FILE_EXTENSION))
                .thenReturn(throwableFile);
        whenNew(File.class).withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), anyString())
                .thenReturn(mock(File.class));
        LogSerializer logSerializer = mock(LogSerializer.class);
        String jsonCrash = "{}";
        when(logSerializer.serializeLog(any(Log.class))).thenReturn(jsonCrash);

        /* Mock storage to fail on stack overflow when saving a Throwable as binary. */
        Throwable throwable = new Throwable();
        doThrow(new StackOverflowError()).when(FileManager.class);
        FileManager.write(eq(throwableFile), eq(STACK_TRACE));

        /* Simulate start SDK. */
        Crashes crashes = Crashes.getInstance();
        crashes.setLogSerializer(logSerializer);
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);

        /* Simulate crash. */
        Crashes.getInstance().saveUncaughtException(Thread.currentThread(), throwable);

        /* Verify it didn't prevent saving the JSON file. */
        verifyStatic(FileManager.class);
        FileManager.write(any(File.class), eq(jsonCrash));
    }

    @Test
    public void handlerMemoryWarning() {

        /* Mock files. */
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mTemporaryFolder.getRoot());
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);

        /* Mock classes. */
        Context mockContext = mock(Context.class);
        ArgumentCaptor<ComponentCallbacks2> componentCallbacks2Captor = ArgumentCaptor.forClass(ComponentCallbacks2.class);
        doNothing().when(mockContext).registerComponentCallbacks(componentCallbacks2Captor.capture());

        /* Instance crash module. */
        Crashes crashes = Crashes.getInstance();
        crashes.onStarted(mockContext, mock(Channel.class), "", null, true);
        crashes.applyEnabledState(true);
        componentCallbacks2Captor.getValue().onConfigurationChanged(mock(Configuration.class));

        /* Invoke callback onTrimMemory. */
        componentCallbacks2Captor.getValue().onTrimMemory(TRIM_MEMORY_RUNNING_CRITICAL);

        /* Verify put data to preferences. */
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREF_KEY_MEMORY_RUNNING_LEVEL), eq(TRIM_MEMORY_RUNNING_CRITICAL));

        /* Invoke callback onLowMemory. */
        componentCallbacks2Captor.getValue().onLowMemory();

        /* Verify put data to preferences. */
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREF_KEY_MEMORY_RUNNING_LEVEL), eq(TRIM_MEMORY_COMPLETE));
    }

    @Test
    public void registerAndUnregisterComponentCallbacks() {

        /* Mock classes. */
        Context mockContext = mock(Context.class);
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mTemporaryFolder.getRoot());
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);

        /* Instance crash module. */
        Crashes crashes = Crashes.getInstance();
        crashes.setInstanceEnabled(false);
        crashes.onStarted(mockContext, mock(Channel.class), "", null, true);

        /* Verify register callback. */
        verify(mockContext, never()).registerComponentCallbacks(any(ComponentCallbacks.class));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(eq(PREF_KEY_MEMORY_RUNNING_LEVEL));

        /* Enable crashes. */
        crashes.setInstanceEnabled(true);
        verify(mockContext).registerComponentCallbacks(isA(ComponentCallbacks2.class));

        /* Disable crashes. */
        crashes.setInstanceEnabled(false);

        /* Verify unregister callback. */
        verify(mockContext).unregisterComponentCallbacks(isA(ComponentCallbacks2.class));

        /* Verify clear preferences. It's called first time during starting disabled service. */
        verifyStatic(SharedPreferencesManager.class, times(2));
        SharedPreferencesManager.remove(eq(PREF_KEY_MEMORY_RUNNING_LEVEL));
    }

    @Test
    public void setReceiveMemoryWarningInLastSession() {
        mockStatic(ErrorLogHelper.class);
        when(ErrorLogHelper.getErrorStorageDirectory()).thenReturn(mTemporaryFolder.getRoot());
        when(ErrorLogHelper.getStoredErrorLogFiles()).thenReturn(new File[0]);
        when(ErrorLogHelper.getNewMinidumpFiles()).thenReturn(new File[0]);
        when(FileManager.read(any(File.class))).thenReturn("");

        when(SharedPreferencesManager.getInt(eq(PREF_KEY_MEMORY_RUNNING_LEVEL), anyInt()))
                .thenReturn(TRIM_MEMORY_UI_HIDDEN);
        checkHasReceivedMemoryWarningInLastSession(false);

        when(SharedPreferencesManager.getInt(eq(PREF_KEY_MEMORY_RUNNING_LEVEL), anyInt()))
                .thenReturn(TRIM_MEMORY_RUNNING_LOW);
        checkHasReceivedMemoryWarningInLastSession(true);

        when(SharedPreferencesManager.getInt(eq(PREF_KEY_MEMORY_RUNNING_LEVEL), anyInt()))
                .thenReturn(TRIM_MEMORY_RUNNING_CRITICAL);
        checkHasReceivedMemoryWarningInLastSession(true);

        when(SharedPreferencesManager.getInt(eq(PREF_KEY_MEMORY_RUNNING_LEVEL), anyInt()))
                .thenReturn(TRIM_MEMORY_COMPLETE);
        checkHasReceivedMemoryWarningInLastSession(true);

        when(SharedPreferencesManager.getInt(eq(PREF_KEY_MEMORY_RUNNING_LEVEL), anyInt()))
                .thenReturn(TRIM_MEMORY_RUNNING_MODERATE);
        checkHasReceivedMemoryWarningInLastSession(true);
    }

    @Test
    public void checkStacktraceWhenThrowableFileIsEmpty() throws Exception {
        String expectedStacktrace = "type: message\n" +
                "\t at ClassName.MethodName(FileName:1)";

        /* Create empty throwable file. */
        UUID logId = UUID.randomUUID();
        String throwableFileName = logId + ErrorLogHelper.THROWABLE_FILE_EXTENSION;
        File errorStorageDirectory = mTemporaryFolder.newFolder("error");
        ErrorLogHelper.setErrorLogDirectory(errorStorageDirectory);
        File throwableFile = new File(errorStorageDirectory, throwableFileName);
        assertTrue(throwableFile.createNewFile());

        /* Prepare first frame. */
        final StackFrame stackFrame1 = new StackFrame();
        stackFrame1.setClassName("ClassName");
        stackFrame1.setMethodName("MethodName");
        stackFrame1.setFileName("FileName");
        stackFrame1.setLineNumber(1);

        /* Mock exception value. */
        com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = mock(com.microsoft.appcenter.crashes.ingestion.models.Exception.class);
        when(mockException.getType()).thenReturn("type");
        when(mockException.getMessage()).thenReturn("message");
        when(mockException.getFrames()).thenReturn(Collections.singletonList(stackFrame1));

        /* Mock log. */
        ManagedErrorLog mockLog = new ManagedErrorLog();
        mockLog.setId(logId);
        mockLog.setErrorThreadName("Thread name");
        mockLog.setAppLaunchTimestamp(new Date());
        mockLog.setTimestamp(new Date());
        mockLog.setDevice(new Device());
        mockLog.setException(mockException);

        /* Build error report. */
        Crashes crashes = Crashes.getInstance();

        /* Verify that stacktrace is null. */
        ErrorReport report = crashes.buildErrorReport(mockLog);
        assertEquals(expectedStacktrace, report.getStackTrace());
        verifyStatic(FileManager.class, never());
        FileManager.read(any(File.class));
    }

    @Test
    public void checkStacktraceWithLegacyThrowableFile() throws Exception {
        String expectedStacktrace = "type: message\n" +
                " ClassName.MethodName(FileName:1)";

        /* Mock FileManager call. */
        when(FileManager.read(any(File.class))).thenReturn(expectedStacktrace);

        /* Create throwable file. */
        UUID logId = UUID.randomUUID();
        String throwableFileName = logId + ErrorLogHelper.THROWABLE_FILE_EXTENSION;
        File errorStorageDirectory = mTemporaryFolder.newFolder("error");
        ErrorLogHelper.setErrorLogDirectory(errorStorageDirectory);
        File throwableFile = new File(errorStorageDirectory, throwableFileName);
        assertTrue(throwableFile.createNewFile());

        /* Write file content. */
        BufferedWriter writer = new BufferedWriter(new FileWriter(throwableFile));
        writer.write(expectedStacktrace);
        writer.close();

        /* Mock log. */
        ManagedErrorLog mockLog = new ManagedErrorLog();
        mockLog.setId(logId);
        mockLog.setErrorThreadName("Thread name");
        mockLog.setAppLaunchTimestamp(new Date());
        mockLog.setTimestamp(new Date());
        mockLog.setDevice(new Device());

        /* Build error report. */
        Crashes crashes = Mockito.spy(Crashes.getInstance());
        ErrorReport report = crashes.buildErrorReport(mockLog);

        /* Verify. */
        assertEquals(expectedStacktrace, report.getStackTrace());
        verify(crashes, never()).buildStackTrace(any(com.microsoft.appcenter.crashes.ingestion.models.Exception.class));
        verifyStatic(FileManager.class);
        FileManager.read(any(File.class));
    }

    @Test
    public void checkBuildStacktrace() {
        String expectedStacktrace = "Type of exception: Message exception\n" +
                "\t at ClassName.MethodName(FileName:1)\n" +
                "\t at ClassName.MethodName(FileName:2)";
        com.microsoft.appcenter.crashes.ingestion.models.Exception mockException = new com.microsoft.appcenter.crashes.ingestion.models.Exception();
        mockException.setType("Type of exception");
        mockException.setMessage("Message exception");

        /* Prepare first frame. */
        final StackFrame stackFrame1 = new StackFrame();
        stackFrame1.setClassName("ClassName");
        stackFrame1.setMethodName("MethodName");
        stackFrame1.setFileName("FileName");
        stackFrame1.setLineNumber(1);

        /* Prepare second frame. */
        final StackFrame stackFrame2 = new StackFrame();
        stackFrame2.setClassName("ClassName");
        stackFrame2.setMethodName("MethodName");
        stackFrame2.setFileName("FileName");
        stackFrame2.setLineNumber(2);
        mockException.setFrames(Arrays.asList(stackFrame1, stackFrame2));
        String stacktrace = Crashes.getInstance().buildStackTrace(mockException);
        assertEquals(expectedStacktrace, stacktrace);
    }

    private void checkHasReceivedMemoryWarningInLastSession(boolean expected) {
        Crashes crashes = Crashes.getInstance();
        crashes.onStarting(mAppCenterHandler);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);
        crashes.setInstanceEnabled(true);
        crashes.onStarted(mock(Context.class), mock(Channel.class), "", null, true);
        assertEquals(expected, Crashes.hasReceivedMemoryWarningInLastSession().get());
        crashes.setInstanceEnabled(false);
    }
}
