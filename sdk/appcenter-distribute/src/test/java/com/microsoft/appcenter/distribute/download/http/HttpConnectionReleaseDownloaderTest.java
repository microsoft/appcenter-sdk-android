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
import com.microsoft.appcenter.utils.AppCenterLog;
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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
        HttpConnectionReleaseDownloader.class,
        SharedPreferencesManager.class,
        PermissionUtils.class,
        NetworkStateHelper.class,
        AsyncTaskUtils.class,
        DownloadManager.Request.class,
        Uri.class,
        Build.VERSION.class
})
public class HttpConnectionReleaseDownloaderTest {

    private static String STUB_DIRECTORY_PATH = "folder/folder/";

    private static String STUB_FILENAME = "file";

    private static String STUB_FILE_EXTENSION = ".apk";

    private static String STUB_FILE_PATH = STUB_FILENAME + STUB_FILE_EXTENSION;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Context mContext;

    private ReleaseDownloader.Listener mockListener;

    private NetworkStateHelper mNetworkStateHelper;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
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
    public void noTargetFile() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mContext.getExternalFilesDir(anyString())).thenReturn(null);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);

        /* Start downloader. */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();

        /* Verify an error. */
        verify(mockListener).onError("Cannot access to downloads folder. Shared storage is not currently available.");
    }

    @Test
    public void noNetwork() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);

        /* Mock no network. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);

        /* Start downloader. */
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();

        /* Verify an error. */
        verify(mockListener).onError("No network connection, abort downloading.");
    }

    @Test
    public void noExternalStoragePermissionApi19() throws Exception {
        testNoExternalStoragePermission(19);
    }

    @Test
    public void noExternalStoragePermissionApi16() throws Exception {
        testNoExternalStoragePermission(16);
    }

    @Test
    public void downloadStart() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        mockShowProgress();

        /* Start downloader. */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();

        /* Verify onStart() was called. */
        verify(mockListener).onStart(anyLong());
    }

    @Test
    public void downloadInProgress() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        HttpConnectionDownloadFileTask mockHttpTask = mock(HttpConnectionDownloadFileTask.class);
        HttpConnectionCheckTask mockHttpCheckTask = mock(HttpConnectionCheckTask.class);
        mockStatic(AsyncTaskUtils.class);
        when(AsyncTaskUtils.execute(anyString(), any(HttpConnectionDownloadFileTask.class))).thenReturn(mockHttpTask);
        when(AsyncTaskUtils.execute(anyString(), any(HttpConnectionCheckTask.class))).thenReturn(mockHttpCheckTask);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        mockShowProgress();

        /* Start downloading twice. */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onStart(anyLong());
        releaseDownloader.resume();

        /* Verify that onStart() was called once. */
        verify(mockListener).onStart(anyLong());
    }

    @Test
    public void onDownloadProgress() throws Exception {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        mockShowProgress();

        /* Call onDownloadProgress(). */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.onDownloadProgress((long) 1, (long) 100);

        /* Verify Listener was called. */
        verify(mockListener).onProgress(eq((long) 1), eq((long) 100));

        /* Assert getter. */
        assertEquals(mockReleaseDetails, releaseDownloader.getReleaseDetails());
    }

    @Test
    public void onDownloadError() {
        String someError = "Some error";
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        mockCancelProgress();

        /* Call onDownloadError(). */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.onDownloadError(someError);

        /* Verify onError() was called. */
        verify(mockListener).onError(eq(someError));
    }

    @Test
    public void onDownloadComplete() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getSize()).thenReturn(100L);
        File mockTargetFile = mock(File.class);
        when(mockTargetFile.length()).thenReturn(100L);
        when(mockTargetFile.getAbsolutePath()).thenReturn(STUB_DIRECTORY_PATH + STUB_FILE_PATH);
        mockCancelProgress();

        /* Call onDownloadComplete(). */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.onDownloadComplete(mockTargetFile);

        /* Verify onComplete() was called. */
        verify(mockListener).onComplete(any(Uri.class));
    }

    @Test
    public void onDownloadCompleteError() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getSize()).thenReturn(150L);
        File targetFile = mock(File.class);
        when(targetFile.length()).thenReturn(100L);
        mockCancelProgress();

        /* Call onDownloadComplete() when the releaseDetails has different file size than downloaded file. */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.onDownloadComplete(targetFile);

        /* Verify onError() was called. */
        verify(mockListener).onError(eq("Downloaded file has incorrect size."));
    }

    @Test
    public void downloadComplete() throws Exception {

        /* Mock Release Details. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getReleaseHash()).thenReturn(STUB_FILENAME);

        /* Mock Directory file. */
        File mockDirectoryFile = mock(File.class);
        when(mockDirectoryFile.getAbsolutePath()).thenReturn(STUB_DIRECTORY_PATH);
        when(mContext.getExternalFilesDir(anyString())).thenReturn(mockDirectoryFile);

        /* Mock Target file. */
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(STUB_FILE_PATH);
        when(mockFile.exists()).thenReturn(true);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(mockDirectoryFile, STUB_FILE_PATH)
                .thenReturn(mockFile);

        /* Mock if Target file exists. */
        whenNew(File.class)
                .withParameterTypes(String.class)
                .withArguments(STUB_FILE_PATH)
                .thenReturn(mockFile);

        /* Mock other calls. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(STUB_FILE_PATH);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);

        /* Start downloader. */
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener));
        releaseDownloader.resume();

        /* Verify. */
        verify(releaseDownloader).onDownloadComplete(any(File.class));
    }

    @Test
    public void testDownloadCompleteButFileNotExists() throws Exception {
        mockShowProgress();

        /* Mock Release Details. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getReleaseHash()).thenReturn(STUB_FILENAME);

        /* Mock Directory file. */
        File mockDirectoryFile = mock(File.class);
        when(mockDirectoryFile.getAbsolutePath()).thenReturn(STUB_DIRECTORY_PATH);
        when(mContext.getExternalFilesDir(anyString())).thenReturn(mockDirectoryFile);

        /* Mock Target file. */
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(STUB_FILE_PATH);
        when(mockFile.exists()).thenReturn(false);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(mockDirectoryFile, STUB_FILE_PATH)
                .thenReturn(mockFile);

        /* Mock if Target file exists. */
        whenNew(File.class)
                .withParameterTypes(String.class)
                .withArguments(STUB_FILE_PATH)
                .thenReturn(mockFile);

        /* Mock other calls. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(STUB_FILE_PATH);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);

        /* Start downloader. */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), any(HttpConnectionDownloadFileTask.class));
    }

    @Test
    public void downloadedFileNotEqual() throws Exception {
        String oldFilePath = "old" + STUB_FILENAME + STUB_FILE_EXTENSION;

        /* Mock Release Details. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getReleaseHash()).thenReturn(STUB_FILENAME);

        /* Mock Directory file. */
        File mockDirectoryFile = mock(File.class);
        when(mockDirectoryFile.getAbsolutePath()).thenReturn(STUB_DIRECTORY_PATH);
        when(mContext.getExternalFilesDir(anyString())).thenReturn(mockDirectoryFile);

        /* Mock Target file. */
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(STUB_FILE_PATH);
        when(mockFile.exists()).thenReturn(true);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(mockDirectoryFile, STUB_FILE_PATH)
                .thenReturn(mockFile);

        /* Mock if Target file exists. */
        whenNew(File.class)
                .withParameterTypes(String.class)
                .withArguments(STUB_FILE_PATH)
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
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);
        mockShowProgress();

        /* Start downloader. */
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();

        /* Verify onStart() was called. */
        verify(mockListener).onStart(anyLong());

        /* To cover "downloading is already in progress" debug log. */
        releaseDownloader.resume();

        /* Verify. */
        verifyStatic(times(1));
        AppCenterLog.debug(anyString(), anyString());
    }

    @Test
    public void cancel() {
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        NotificationManager mockNotificationManager = mockCancelProgress();

        /* Call cancel() downloading. */
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener));
        releaseDownloader.cancel();

        /* Verify that notification was cancelled. */
        verify(mockNotificationManager).cancel(anyInt());
    }

    @Test
    public void cancelRemovingFile() {
        NotificationManager mockNotificationManager = mockCancelProgress();

        /* Mock the file was downloaded. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(STUB_DIRECTORY_PATH + STUB_FILE_PATH);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);

        /* Call cancel() downloading. */
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener));
        releaseDownloader.cancel();

        /* Verify that notification was cancelled. */
        verify(mockNotificationManager).cancel(anyInt());
    }

    @Test
    public void cancelDownloading() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        HttpConnectionDownloadFileTask mockHttpTask = mock(HttpConnectionDownloadFileTask.class);
        HttpConnectionCheckTask mockCheckTask = mock(HttpConnectionCheckTask.class);
        mockStatic(AsyncTaskUtils.class);
        when(AsyncTaskUtils.execute(anyString(), any(HttpConnectionDownloadFileTask.class))).thenReturn(mockHttpTask);
        when(AsyncTaskUtils.execute(anyString(), any(HttpConnectionCheckTask.class))).thenReturn(mockCheckTask);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        mockShowProgress();
        NotificationManager mockNotificationManager = mockCancelProgress();

        /* Call cancel() downloading. */
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener));
        releaseDownloader.resume();
        releaseDownloader.cancel();

        /* Verify that notification was cancelled. */
        verify(mockNotificationManager).cancel(anyInt());
    }

    private void testNoExternalStoragePermission(int apiLevel) throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(false);

        /* To cover api-level-dependant code. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", apiLevel);

        /* Start downloader. */
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();

        /* Verify onError() was called. */
        verify(mockListener).onError("No external storage permission.");
    }

    private ReleaseDetails mockTargetFile() throws Exception {
        String filePath = STUB_FILENAME + STUB_FILE_EXTENSION;

        /* Mock Release Details. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getReleaseHash()).thenReturn(STUB_FILENAME);

        /* Mock Directory file. */
        File mockDirectoryFile = mock(File.class);
        when(mockDirectoryFile.getAbsolutePath()).thenReturn(STUB_DIRECTORY_PATH);
        when(mContext.getExternalFilesDir(anyString())).thenReturn(mockDirectoryFile);

        /* Mock Target file. */
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(filePath);
        when(mockFile.exists()).thenReturn(true);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(mockDirectoryFile, filePath)
                .thenReturn(mockFile);
        return mockReleaseDetails;
    }

    private void mockShowProgress() throws Exception {
        ApplicationInfo mockApplicationInfo = mock(ApplicationInfo.class);
        mockApplicationInfo.icon = 1;
        when(mContext.getApplicationInfo()).thenReturn(mockApplicationInfo);
        Notification.Builder mockNotificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class)
                .withParameterTypes(Context.class)
                .withArguments(mContext)
                .thenReturn(mockNotificationBuilder);
        when(mockNotificationBuilder.setContentTitle(anyString())).thenReturn(mockNotificationBuilder);
        when(mockNotificationBuilder.setSmallIcon(anyInt())).thenReturn(mockNotificationBuilder);
        when(mockNotificationBuilder.setProgress(anyInt(), anyInt(), anyBoolean())).thenReturn(mockNotificationBuilder);
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);
    }

    private NotificationManager mockCancelProgress() {
        NotificationManager mockNotificationManager = mock(NotificationManager.class);
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);
        return mockNotificationManager;
    }
}
