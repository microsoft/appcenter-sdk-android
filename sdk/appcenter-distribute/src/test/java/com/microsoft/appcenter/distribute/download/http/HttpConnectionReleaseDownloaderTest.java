/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;

import com.microsoft.appcenter.distribute.PermissionUtils;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.Returns;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({HttpConnectionReleaseDownloader.class, SharedPreferencesManager.class, PermissionUtils.class, NetworkStateHelper.class, AsyncTaskUtils.class, DownloadManager.Request.class, Uri.class, Build.VERSION.class})
public class HttpConnectionReleaseDownloaderTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();
    private Context mockContext;
    private ReleaseDownloader.Listener mockListener;
    private NetworkStateHelper mNetworkStateHelper;

    @Before
    public void setUp() {
        mockContext = mock(Context.class);
        mockListener = mock(ReleaseDownloader.Listener.class);
        mockStatic(NetworkStateHelper.class);
        mNetworkStateHelper = mock(NetworkStateHelper.class, new Returns(true));
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(mNetworkStateHelper);
        mockStatic(SharedPreferencesManager.class);
        mockStatic(AsyncTaskUtils.class);
        mockStatic(Uri.class);
        mockStatic(PermissionUtils.class);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }

    @Test
    public void testNoTargetFile() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockContext.getExternalFilesDir(anyString())).thenReturn(null);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError("Cannot access to downloads folder. Shared storage is not currently available.");
    }

    @Test
    public void testNoNetwork() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError("No network connection, abort downloading.");
    }

    @Test
    public void testNoExternalStoragePermissionApi19() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 19);
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(false);
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError("No external storage permission.");
    }

    @Test
    public void testNoExternalStoragePermissionApi16() throws Exception {
        testNoExternalStoragePermission(16);
    }

    @Test
    public void testDownloadStart() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        mockShowProgress();
        releaseDownloader.resume();
        verify(mockListener).onStart(anyLong());
    }

    @Test
    public void testDownloadInProgress() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        HttpDownloadFileTask mockHttpTask = mock(HttpDownloadFileTask.class);
        mockStatic(AsyncTaskUtils.class);
        when(AsyncTaskUtils.execute(anyString(), any(HttpDownloadFileTask.class))).thenReturn(mockHttpTask);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        mockShowProgress();
        releaseDownloader.resume();
        releaseDownloader.resume();

        /* Verify that was called once. */
        verify(mockListener).onStart(anyLong());
    }

    @Test
    public void testOnDownloadProgress() throws Exception {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        mockShowProgress();
        releaseDownloader.onDownloadProgress((long) 1, (long) 100);
        verify(mockListener).onProgress(eq((long) 1), eq((long) 100));
    }

    @Test
    public void testOnDownloadError() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        mockCancelProgress();
        String someError = "Some error";
        releaseDownloader.onDownloadError(someError);
        verify(mockListener).onError(eq(someError));
    }

    @Test
    public void testOnDownloadComplete() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getSize()).thenReturn(100L);
        File mockTargetFile = mock(File.class);
        when(mockTargetFile.length()).thenReturn(100L);
        when(mockTargetFile.getAbsolutePath()).thenReturn("/folder/folder/file.apk");
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        mockCancelProgress();
        releaseDownloader.onDownloadComplete(mockTargetFile);
        verify(mockListener).onComplete(any(Uri.class));
    }

    @Test
    public void testOnDownloadCompleteError() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getSize()).thenReturn(150L);
        File targetFile = mock(File.class);
        when(targetFile.length()).thenReturn(100L);
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        mockCancelProgress();
        releaseDownloader.onDownloadComplete(targetFile);
        verify(mockListener).onError(eq("Downloaded file has incorrect size."));
    }

    @Test
    public void testDownloadComplete() throws Exception {
        String directoryPath = "folder/folder/";
        String fileName = "file";
        String fileExt = ".apk";
        String filePath = fileName + fileExt;

        /* Mock Release Details. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getReleaseHash()).thenReturn(fileName);

        /* Mock Directory file. */
        File mockDirectoryFile = mock(File.class);
        when(mockDirectoryFile.getAbsolutePath()).thenReturn(directoryPath);
        when(mockContext.getExternalFilesDir(anyString())).thenReturn(mockDirectoryFile);

        /* Mock Target file. */
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(filePath);
        when(mockFile.exists()).thenReturn(true);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(mockDirectoryFile, "file.apk")
                .thenReturn(mockFile);

        /* Mock if Target file exists. */
        whenNew(File.class)
                .withParameterTypes(String.class)
                .withArguments(filePath)
                .thenReturn(mockFile);

        /* Mock other calls. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(filePath);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);

        /* Run and verify. */
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener));
        releaseDownloader.resume();
        verify(releaseDownloader).onDownloadComplete(any(File.class));
    }

    @Test
    public void testDownloadedFileNotEqual() throws Exception {
        String directoryPath = "folder/folder/";
        String fileName = "file";
        String fileExt = ".apk";
        String filePath = fileName + fileExt;
        String oldFilePath = "old" + fileName + fileExt;

        /* Mock Release Details. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getReleaseHash()).thenReturn(fileName);

        /* Mock Directory file. */
        File mockDirectoryFile = mock(File.class);
        when(mockDirectoryFile.getAbsolutePath()).thenReturn(directoryPath);
        when(mockContext.getExternalFilesDir(anyString())).thenReturn(mockDirectoryFile);

        /* Mock Target file. */
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(filePath);
        when(mockFile.exists()).thenReturn(true);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(mockDirectoryFile, "file.apk")
                .thenReturn(mockFile);

        /* Mock if Target file exists. */
        whenNew(File.class)
                .withParameterTypes(String.class)
                .withArguments(filePath)
                .thenReturn(mockFile);

        /* Mock if Target file exists. */
        File mockOldFile = mock(File.class);
        when(mockOldFile.getAbsolutePath()).thenReturn(oldFilePath);
        whenNew(File.class)
                .withParameterTypes(String.class)
                .withArguments(oldFilePath)
                .thenReturn(mockOldFile);

        /* Mock other calls. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(oldFilePath);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);

        /* Run and verify. */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        mockShowProgress();
        releaseDownloader.resume();
        verify(mockListener).onStart(anyLong());
        releaseDownloader.resume();
    }

    @Test
    public void testCancel() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        NotificationManager mockNotificationManager = mockCancelProgress();
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener));
        releaseDownloader.cancel();
        verify(mockNotificationManager).cancel(anyInt());
    }

    @Test
    public void testCancelRemovingFile() {
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn("folder/folder/file.apk");
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        NotificationManager mockNotificationManager = mockCancelProgress();
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener));
        releaseDownloader.cancel();
        verify(mockNotificationManager).cancel(anyInt());
    }

    private void testNoExternalStoragePermission(int apiLevel) throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", apiLevel);
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(false);
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError("No external storage permission.");
    }

    private ReleaseDetails mockTargetFile() throws Exception {
        String directoryPath = "folder/folder/";
        String fileName = "file";
        String fileExt = ".apk";
        String filePath = fileName + fileExt;

        /* Mock Release Details. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getReleaseHash()).thenReturn(fileName);

        /* Mock Directory file. */
        File mockDirectoryFile = mock(File.class);
        when(mockDirectoryFile.getAbsolutePath()).thenReturn(directoryPath);
        when(mockContext.getExternalFilesDir(anyString())).thenReturn(mockDirectoryFile);

        /* Mock Target file. */
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(filePath);
        when(mockFile.exists()).thenReturn(true);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(mockDirectoryFile, "file.apk")
                .thenReturn(mockFile);
        return mockReleaseDetails;
    }

    private void mockShowProgress() throws Exception {
        ApplicationInfo mockApplicationInfo = mock(ApplicationInfo.class);
        mockApplicationInfo.icon = 1;
        when(mockContext.getApplicationInfo()).thenReturn(mockApplicationInfo);
        Notification.Builder mockNotificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class)
                .withParameterTypes(Context.class)
                .withArguments(mockContext)
                .thenReturn(mockNotificationBuilder);
        when(mockNotificationBuilder.setContentTitle(anyString())).thenReturn(mockNotificationBuilder);
        when(mockNotificationBuilder.setSmallIcon(anyInt())).thenReturn(mockNotificationBuilder);
        when(mockNotificationBuilder.setProgress(anyInt(), anyInt(), anyBoolean())).thenReturn(mockNotificationBuilder);
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);
    }

    private NotificationManager mockCancelProgress() {
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);
        return mockNotificationManager;
    }

}
