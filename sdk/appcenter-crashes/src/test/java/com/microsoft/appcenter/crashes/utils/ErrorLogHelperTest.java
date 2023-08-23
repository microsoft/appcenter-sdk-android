/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.utils;

import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.crashes.ingestion.models.Thread;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.crashes.model.TestCrashException;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.storage.FileManager;

import org.json.JSONException;
import org.json.JSONStringer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

@PrepareForTest({
        AppCenterLog.class,
        Build.class,
        DeviceInfoHelper.class,
        ErrorLogHelper.class,
        FileManager.class,
        Process.class,
        TextUtils.class
})
public class ErrorLogHelperTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

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

    @SuppressWarnings("InstantiationOfUtilityClass")
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
        mockStatic(java.lang.Thread.class); // Workaround for PowerMock class loading issue.
        Map<java.lang.Thread, StackTraceElement[]> allStackTraces = java.lang.Thread.getAllStackTraces();
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(
                mockContext,
                java.lang.Thread.currentThread(),
                new RuntimeException(new IOException(new TestCrashException())),
                allStackTraces,
                launchTimeStamp);
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
        assertEquals(allStackTraces.size(), errorLog.getThreads().size());
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
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class)))
                .thenThrow(new DeviceInfoHelper.DeviceInfoException("mock"));

        /* Mock architecture. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 15);
        TestUtils.setInternalState(Build.class, "CPU_ABI", "armeabi-v7a");

        /* Test. */
        long launchTimeStamp = 2000;
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, java.lang.Thread.currentThread(), new java.lang.Exception(), new HashMap<>(), launchTimeStamp);
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
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class)))
                .thenThrow(new DeviceInfoHelper.DeviceInfoException("mock"));

        /* Mock activity manager to return null active processes. */
        ActivityManager activityManager = mock(ActivityManager.class);
        when(activityManager.getRunningAppProcesses()).thenReturn(null);
        when(mockContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager);

        /* Mock architecture. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 15);
        TestUtils.setInternalState(Build.class, "CPU_ABI", "armeabi-v7a");

        /* Test. */
        long launchTimeStamp = 2000;
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, java.lang.Thread.currentThread(), new java.lang.Exception(), new HashMap<>(), launchTimeStamp);
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
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mockContext, java.lang.Thread.currentThread(), new RuntimeException(new TestCrashException()), new HashMap<>(), 900);
        assertNotNull(errorLog);

        /* Test. */
        String stackTrace = "Sample stack trace";
        ErrorReport report = ErrorLogHelper.getErrorReportFromErrorLog(errorLog, stackTrace);
        assertNotNull(report);
        assertEquals(errorLog.getId().toString(), report.getId());
        assertEquals(errorLog.getErrorThreadName(), report.getThreadName());
        assertEquals(stackTrace, report.getStackTrace());
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
    public void cleanDirectory() throws java.lang.Exception {

        /* Prepare data. */
        mockStatic(FileManager.class);
        File errorLogFolder = mTemporaryFolder.newFolder("errorLogFolder");
        ErrorLogHelper.setErrorLogDirectory(errorLogFolder);

        /* Clean pending minidump. */
        ErrorLogHelper.cleanPendingMinidumps();

        /* Verify clean function was called. */
        verifyStatic(FileManager.class);
        FileManager.cleanDirectory(ErrorLogHelper.getPendingMinidumpDirectory());

        /* Clean up. */
        ErrorLogHelper.setErrorLogDirectory(null);
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

    @Test
    public void getStoredDeviceInfo() throws IOException {
        String deviceInfoString = "{\"sdkName\":\"appcenter.android\",\"sdkVersion\":\"2.5.4.2\",\"model\":\"Android SDK built for x86\",\"oemName\":\"Google\",\"osName\":\"Android\",\"osVersion\":\"9\",\"osBuild\":\"PSR1.180720.075\",\"osApiLevel\":28,\"locale\":\"en_US\",\"timeZoneOffset\":240,\"screenSize\":\"1080x1794\",\"appVersion\":\"2.5.4.2\",\"carrierName\":\"Android\",\"carrierCountry\":\"us\",\"appBuild\":\"59\",\"appNamespace\":\"com.microsoft.appcenter.sasquatch.project\"}";
        File minidumpFolder = mTemporaryFolder.newFolder("minidump");
        File deviceInfoFile = new File(minidumpFolder, ErrorLogHelper.DEVICE_INFO_FILE);
        assertTrue(deviceInfoFile.createNewFile());
        mockStatic(FileManager.class);
        when(FileManager.read(eq(deviceInfoFile))).thenReturn(deviceInfoString);
        Device storedDeviceInfo = ErrorLogHelper.getStoredDeviceInfo(minidumpFolder);
        assertNotNull(storedDeviceInfo);
    }

    @Test
    public void getStoredMinidumpFileContentNull() {
        File minidumpFolder = mock(File.class);
        when(minidumpFolder.listFiles(any(FilenameFilter.class))).thenReturn(null);
        Device storedDeviceInfo = ErrorLogHelper.getStoredDeviceInfo(minidumpFolder);
        String storedUserId = ErrorLogHelper.getStoredUserInfo(minidumpFolder);
        String dataResidencyRegion = ErrorLogHelper.getStoredDataResidencyRegion(minidumpFolder);
        assertNull(storedDeviceInfo);
        assertNull(storedUserId);
        assertNull(dataResidencyRegion);
    }

    @Test
    public void getStoredMinidumpFileContentEmpty() throws IOException {
        File minidumpFolder = mTemporaryFolder.newFolder("minidump");
        Device storedDeviceInfo = ErrorLogHelper.getStoredDeviceInfo(minidumpFolder);
        String storedUserId = ErrorLogHelper.getStoredUserInfo(minidumpFolder);
        String dataResidencyRegion = ErrorLogHelper.getStoredDataResidencyRegion(minidumpFolder);
        assertNull(storedDeviceInfo);
        assertNull(storedUserId);
        assertNull(dataResidencyRegion);
    }

    @Test
    public void getStoredMinidumpFileContentCannotRead() throws IOException {
        File minidumpFolder = mTemporaryFolder.newFolder("minidump");
        File deviceInfoFile = new File(minidumpFolder, ErrorLogHelper.DEVICE_INFO_FILE);
        assertTrue(deviceInfoFile.createNewFile());
        mockStatic(FileManager.class);
        when(FileManager.read(eq(deviceInfoFile))).thenReturn(null);
        Device storedDeviceInfo = ErrorLogHelper.getStoredDeviceInfo(minidumpFolder);
        assertNull(storedDeviceInfo);
        String userInfo = ErrorLogHelper.getStoredUserInfo(minidumpFolder);
        assertNull(userInfo);
        String dataResidencyRegion = ErrorLogHelper.getStoredDataResidencyRegion(minidumpFolder);
        assertNull(dataResidencyRegion);
    }

    @Test
    public void throwIOExceptionWhenGetMinidumpSubfolderWithDeviceInfo() throws java.lang.Exception {

        /* Prepare data. */
        Device mockDevice = mock(Device.class);
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mockDevice);
        Context mockContext = mock(Context.class);
        File mockFile = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(mockFile);
        mockStatic(FileManager.class);
        doThrow(new IOException()).when(FileManager.class);
        FileManager.write(eq(mockFile), any());

        /* Verify. */
        ErrorLogHelper.getNewMinidumpSubfolderWithContextData(mockContext);
        verify(mockFile).delete();
    }

    @Test
    public void throwDeviceInfoExceptionWhenGetMinidumpSubfolderWithDeviceInfo() throws java.lang.Exception {

        /* Prepare data. */
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class)))
                .thenThrow(new DeviceInfoHelper.DeviceInfoException("crash"));
        Context mockContext = mock(Context.class);
        File mockFile = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(mockFile);

        /* Verify. */
        ErrorLogHelper.getNewMinidumpSubfolderWithContextData(mockContext);
        verify(mockFile).delete();
    }

    @Test
    public void throwJSONExceptionWhenGetMinidumpSubfolderWithDeviceInfo() throws java.lang.Exception {

        /* Prepare data. */
        Device mockDevice = mock(Device.class);
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mockDevice);
        Context mockContext = mock(Context.class);
        File mockFile = mock(File.class);
        whenNew(File.class).withAnyArguments().thenReturn(mockFile);
        doThrow(new JSONException("crash", new java.lang.Exception())).when(mockDevice).write(any(JSONStringer.class));

        /* Verify. */
        ErrorLogHelper.getNewMinidumpSubfolderWithContextData(mockContext);
        verify(mockFile).delete();
    }

    @Test
    public void parseLogFolderUuid() throws IOException {
        String originalFolderName = "a80da2ae-8c85-43b0-a25b-d52319fb6d56";
        File logFolder = mTemporaryFolder.newFolder("new", originalFolderName);
        UUID uuid = ErrorLogHelper.parseLogFolderUuid(logFolder);
        assertEquals(uuid.toString(), originalFolderName);
    }

    @Test
    public void parseLogFolderUuidFallback() throws IOException {
        String originalFolderName = "a80da2ae-8c85-43b0-a25b-d52319fb6d56";
        File logFile = mTemporaryFolder.newFile(originalFolderName);
        UUID uuid = ErrorLogHelper.parseLogFolderUuid(logFile);
        assertNotEquals(uuid.toString(), originalFolderName);
    }

    @Test
    public void parseLogFolderUuidIllegalArgument() throws IOException {
        String originalFolderName = "a80da2ae-8c85-43b0-a25b-d52319fb6d56";
        File logFolder = mTemporaryFolder.newFolder("new", originalFolderName + ".dmp");
        UUID uuid = ErrorLogHelper.parseLogFolderUuid(logFolder);
        assertNotEquals(uuid.toString(), originalFolderName);
    }

    @Test
    public void removeLostThrowableFiles() {

        /* Mock FileManager class. */
        mockStatic(FileManager.class);

        /* Mock files. */
        File mockFile1 = mock(File.class);
        File mockFile2 = mock(File.class);
        when(mockFile1.getName()).thenReturn("74aa0682-3478-11eb-adc1-0242ac120002" + ErrorLogHelper.THROWABLE_FILE_EXTENSION);
        when(mockFile2.getName()).thenReturn("74aa0682-3478-11eb-adc1-0242ac120003" + ErrorLogHelper.THROWABLE_FILE_EXTENSION);

        /* Mock getting storage directory. */
        File mockDir = mock(File.class);
        File[] mockFiles = new File[]{mockFile1, mockFile2};
        when(mockDir.listFiles(any(FilenameFilter.class))).thenReturn(mockFiles);
        ErrorLogHelper.setErrorLogDirectory(mockDir);

        /* Verify removing files when getErrorStorageDirectory return some files. */
        ErrorLogHelper.removeLostThrowableFiles();
        verifyStatic(FileManager.class, times(2));
        FileManager.delete(any(File.class));
    }

    @Test
    public void removeLostThrowableFilesWhenListOfFilesIsEmpty() {

        /* Mock FileManager class. */
        mockStatic(FileManager.class);

        /* Mock getting storage directory. */
        File mockDir = mock(File.class);
        when(mockDir.listFiles(any(FilenameFilter.class)))
                .thenReturn(null)
                .thenReturn(new File[0]);
        ErrorLogHelper.setErrorLogDirectory(mockDir);

        /* Verify removing files when getErrorStorageDirectory return null. */
        ErrorLogHelper.removeLostThrowableFiles();
        verifyStatic(FileManager.class, never());
        FileManager.delete(any(File.class));

        /* Verify removing files when getErrorStorageDirectory return 0. */
        ErrorLogHelper.removeLostThrowableFiles();
        verifyStatic(FileManager.class, never());
        FileManager.delete(any(File.class));
    }

    @Test
    public void removeStoredErrorLogFile() throws java.lang.Exception {

        /* Create file. */
        UUID logId = UUID.randomUUID();
        String throwableFileName = logId + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION;
        File errorStorageDirectory = mTemporaryFolder.newFolder("error");
        ErrorLogHelper.setErrorLogDirectory(errorStorageDirectory);
        File errorLogFile = new File(errorStorageDirectory, throwableFileName);
        assertTrue(errorLogFile.createNewFile());
        assertTrue(errorLogFile.exists());

        /* Remove stored log file. */
        ErrorLogHelper.removeStoredErrorLogFile(logId);

        /* Verify. */
        assertFalse(errorLogFile.exists());

        /* Coverage check. */
        ErrorLogHelper.removeStoredErrorLogFile(UUID.randomUUID());
    }
}
