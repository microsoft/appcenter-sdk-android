/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.crashes.ingestion.models.Thread;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.model.TestCrashException;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.DeviceInfoHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({DeviceInfoHelper.class, Process.class, Build.class, ErrorLogHelper.class})
public class ErrorLogHelperTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(DeviceInfoHelper.class);
        mockStatic(Process.class);
    }

    @After
    public void tearDown() throws java.lang.Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
        TestUtils.setInternalState(Build.class, "SUPPORTED_ABIS", null);
        TestUtils.setInternalState(Build.class, "CPU_ABI", null);
    }

    @Test
    public void createErrorLog() throws java.lang.Exception {

        /* Dummy coverage of utils class. */
        new ErrorLogHelper();

        /* Mock base. */
        Context mockContext = mock(Context.class);
        when(Process.myPid()).thenReturn(123);
        Date logTimestamp = new Date(1000L);
        whenNew(Date.class).withNoArguments().thenReturn(logTimestamp);
        whenNew(Date.class).withArguments(anyLong()).thenAnswer(new Answer<Date>() {

            @Override
            public Date answer(InvocationOnMock invocation) {
                return new Date((Long) invocation.getArguments()[0]);
            }
        });

        /* Mock device. */
        Device mockDevice = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mockDevice);

        /* Mock process name. */
        ActivityManager activityManager = mock(ActivityManager.class);
        RunningAppProcessInfo runningAppProcessInfo = new RunningAppProcessInfo(null, 0, null);
        runningAppProcessInfo.pid = 123;
        runningAppProcessInfo.processName = "right.process";
        when(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager);
        when(activityManager.getRunningAppProcesses()).thenReturn(Arrays.asList(mock(RunningAppProcessInfo.class), runningAppProcessInfo));

        /* Mock architecture. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 23);
        TestUtils.setInternalState(Build.class, "SUPPORTED_ABIS", new String[]{"armeabi-v7a", "arm"});

        /* Test. */
        long launchTimeStamp = 2000;
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, java.lang.Thread.currentThread(), new RuntimeException(new IOException(new TestCrashException())), java.lang.Thread.getAllStackTraces(), launchTimeStamp);
        assertNotNull(errorLog);
        assertNotNull(errorLog.getId());
        assertEquals(logTimestamp, errorLog.getTimestamp());
        assertEquals(mockDevice, errorLog.getDevice());
        assertEquals(Integer.valueOf(123), errorLog.getProcessId());
        assertEquals("right.process", errorLog.getProcessName());
        assertNull(errorLog.getParentProcessId());
        assertNull(errorLog.getParentProcessName());
        assertEquals("armeabi-v7a", errorLog.getArchitecture());
        assertEquals((Long) java.lang.Thread.currentThread().getId(), errorLog.getErrorThreadId());
        assertEquals(java.lang.Thread.currentThread().getName(), errorLog.getErrorThreadName());
        assertEquals(Boolean.TRUE, errorLog.getFatal());
        assertEquals(launchTimeStamp, errorLog.getAppLaunchTimestamp().getTime());

        /* Check first exception. */
        Exception topException = errorLog.getException();
        sanityCheck(topException);
        assertEquals(RuntimeException.class.getName(), topException.getType());
        assertNotNull(topException.getMessage());
        assertNotNull(topException.getInnerExceptions());
        assertEquals(1, topException.getInnerExceptions().size());

        /* Check second exception. */
        Exception middleException = topException.getInnerExceptions().get(0);
        sanityCheck(middleException);
        assertEquals(IOException.class.getName(), middleException.getType());
        assertNotNull(middleException.getInnerExceptions());
        assertEquals(1, middleException.getInnerExceptions().size());

        /* Check third exception. */
        Exception rootCauseException = middleException.getInnerExceptions().get(0);
        sanityCheck(rootCauseException);
        assertEquals(TestCrashException.class.getName(), rootCauseException.getType());
        assertNotNull(rootCauseException.getMessage());
        assertNull(rootCauseException.getInnerExceptions());

        /* Check threads. */
        assertNotNull(errorLog.getThreads());
        assertEquals(java.lang.Thread.getAllStackTraces().size(), errorLog.getThreads().size());
        for (Thread thread : errorLog.getThreads()) {
            assertNotNull(thread);
            assertTrue(thread.getId() > 0);
            assertNotNull(thread.getName());
            assertNotNull(thread.getFrames());
            for (StackFrame frame : thread.getFrames()) {
                assertNotNull(frame);
                assertNotNull(frame.getClassName());
                assertNotNull(frame.getMethodName());
            }
        }
    }

    private void sanityCheck(Exception exception) {
        assertNotNull(exception);
        assertNotNull(exception.getType());
        assertNotNull(exception.getFrames());
        assertFalse(exception.getFrames().isEmpty());
        for (StackFrame frame : exception.getFrames()) {
            assertNotNull(frame);
            assertNotNull(frame.getClassName());
            assertNotNull(frame.getMethodName());
        }
    }

    @Test
    public void createErrorLogWithFailedDeviceGetAndNoActivityManager() throws java.lang.Exception {

        /* Mock base. */
        Context mockContext = mock(Context.class);
        when(Process.myPid()).thenReturn(123);
        Date logTimestamp = new Date(1000L);
        whenNew(Date.class).withNoArguments().thenReturn(logTimestamp);
        whenNew(Date.class).withArguments(anyLong()).thenAnswer(new Answer<Date>() {

            @Override
            public Date answer(InvocationOnMock invocation) {
                return new Date((Long) invocation.getArguments()[0]);
            }
        });

        /* Mock device. */
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenThrow(new DeviceInfoHelper.DeviceInfoException("mock", new PackageManager.NameNotFoundException()));

        /* Mock architecture. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 15);
        TestUtils.setInternalState(Build.class, "CPU_ABI", "armeabi-v7a");

        /* Test. */
        long launchTimeStamp = 2000;
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, java.lang.Thread.currentThread(), new java.lang.Exception(), java.lang.Thread.getAllStackTraces(), launchTimeStamp);
        assertNotNull(errorLog);
        assertNotNull(errorLog.getId());
        assertEquals(logTimestamp, errorLog.getTimestamp());
        assertNull(errorLog.getDevice());
        assertEquals(Integer.valueOf(123), errorLog.getProcessId());
        assertEquals("", errorLog.getProcessName());
        assertNull(errorLog.getParentProcessId());
        assertNull(errorLog.getParentProcessName());
        assertEquals("armeabi-v7a", errorLog.getArchitecture());
        assertEquals((Long) java.lang.Thread.currentThread().getId(), errorLog.getErrorThreadId());
        assertEquals(java.lang.Thread.currentThread().getName(), errorLog.getErrorThreadName());
        assertEquals(Boolean.TRUE, errorLog.getFatal());
        assertEquals(launchTimeStamp, errorLog.getAppLaunchTimestamp().getTime());
    }

    @Test
    public void createErrorLogWithFailedDeviceGetAndNullProcesses() throws java.lang.Exception {

        /* Mock base. */
        Context mockContext = mock(Context.class);
        when(Process.myPid()).thenReturn(123);
        Date logTimestamp = new Date(1000L);
        whenNew(Date.class).withNoArguments().thenReturn(logTimestamp);
        whenNew(Date.class).withArguments(anyLong()).thenAnswer(new Answer<Date>() {

            @Override
            public Date answer(InvocationOnMock invocation) {
                return new Date((Long) invocation.getArguments()[0]);
            }
        });

        /* Mock device. */
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenThrow(new DeviceInfoHelper.DeviceInfoException("mock", new PackageManager.NameNotFoundException()));

        /* Mock activity manager to return null active processes. */
        ActivityManager activityManager = mock(ActivityManager.class);
        when(activityManager.getRunningAppProcesses()).thenReturn(null);
        when(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager);

        /* Mock architecture. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 15);
        TestUtils.setInternalState(Build.class, "CPU_ABI", "armeabi-v7a");

        /* Test. */
        long launchTimeStamp = 2000;
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, java.lang.Thread.currentThread(), new java.lang.Exception(), java.lang.Thread.getAllStackTraces(), launchTimeStamp);
        assertNotNull(errorLog);
        assertNotNull(errorLog.getId());
        assertEquals(logTimestamp, errorLog.getTimestamp());
        assertNull(errorLog.getDevice());
        assertEquals(Integer.valueOf(123), errorLog.getProcessId());
        assertEquals("", errorLog.getProcessName());
        assertNull(errorLog.getParentProcessId());
        assertNull(errorLog.getParentProcessName());
        assertEquals("armeabi-v7a", errorLog.getArchitecture());
        assertEquals((Long) java.lang.Thread.currentThread().getId(), errorLog.getErrorThreadId());
        assertEquals(java.lang.Thread.currentThread().getName(), errorLog.getErrorThreadName());
        assertEquals(Boolean.TRUE, errorLog.getFatal());
        assertEquals(launchTimeStamp, errorLog.getAppLaunchTimestamp().getTime());
    }

    @Test
    public void getErrorReportFromErrorLog() throws java.lang.Exception {

        /* Mock base. */
        Context mockContext = mock(Context.class);
        when(Process.myPid()).thenReturn(123);

        /* Mock device. */
        Device mockDevice = mock(Device.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mockDevice);

        /* Mock process name. */
        ActivityManager activityManager = mock(ActivityManager.class);
        RunningAppProcessInfo runningAppProcessInfo = new RunningAppProcessInfo(null, 0, null);
        runningAppProcessInfo.pid = 123;
        runningAppProcessInfo.processName = "right.process";
        when(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager);
        when(activityManager.getRunningAppProcesses()).thenReturn(Arrays.asList(mock(RunningAppProcessInfo.class), runningAppProcessInfo));

        /* Mock architecture. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 23);
        TestUtils.setInternalState(Build.class, "SUPPORTED_ABIS", new String[]{"armeabi-v7a", "arm"});

        /* Create an error log. */
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, java.lang.Thread.currentThread(), new RuntimeException(new TestCrashException()), java.lang.Thread.getAllStackTraces(), 900);
        assertNotNull(errorLog);

        /* Test. */
        Throwable throwable = new RuntimeException();
        ErrorReport report = ErrorLogHelper.getErrorReportFromErrorLog(errorLog, throwable);
        assertNotNull(report);
        assertEquals(errorLog.getId().toString(), report.getId());
        assertEquals(errorLog.getErrorThreadName(), report.getThreadName());
        assertEquals(throwable, report.getThrowable());
        assertEquals(errorLog.getAppLaunchTimestamp(), report.getAppStartTime());
        assertEquals(errorLog.getTimestamp(), report.getAppErrorTime());
        assertEquals(errorLog.getDevice(), report.getDevice());
    }

    @Test
    public void getStoredErrorLogFilesNullCases() {

        /* Mock instance. */
        File mockErrorLogDirectory = mock(File.class);
        when(mockErrorLogDirectory.listFiles(any(FilenameFilter.class))).thenReturn(null);
        ErrorLogHelper.setErrorLogDirectory(mockErrorLogDirectory);

        /* Test getStoredErrorLogFiles. */
        File[] files = ErrorLogHelper.getStoredErrorLogFiles();
        assertNotNull(files);
        assertEquals(0, files.length);

        /* Test getStoredErrorLogFiles. */
        File file = ErrorLogHelper.getStoredErrorLogFile(UUID.randomUUID());
        assertNull(file);

        /* Clean up. */
        ErrorLogHelper.setErrorLogDirectory(null);
    }

    @Test
    public void validateProperties() {
        String logType = "HandledError";
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
        }};
        assertEquals(0, ErrorLogHelper.validateProperties(properties, logType).size());

        properties = new HashMap<String, String>() {{
            for (int i = 0; i < 30; i++) {
                put("valid" + i, "valid");
            }
        }};
        assertEquals(20, ErrorLogHelper.validateProperties(properties, logType).size());

        final String longerMapItem = generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        properties = new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }};
        Map<String, String> actualProperties = ErrorLogHelper.validateProperties(properties, logType);
        String truncatedMapItem = generateString(ErrorLogHelper.MAX_PROPERTY_ITEM_LENGTH, '*');
        assertEquals(1, actualProperties.size());
        assertEquals(truncatedMapItem, actualProperties.get(truncatedMapItem));
    }

    @Test
    public void truncateCauses() {
        RuntimeException e = new RuntimeException();
        for (int i = 0; i < 32; i++) {
            e = new RuntimeException(Integer.valueOf(i).toString(), e);
        }
        int depth = 1;
        Exception model = ErrorLogHelper.getModelExceptionFromThrowable(e);
        while (model.getInnerExceptions() != null && (model = model.getInnerExceptions().get(0)) != null) {
            depth++;
        }
        assertEquals(ErrorLogHelper.CAUSE_LIMIT, depth);
    }
}
