/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.microsoft.appcenter.distribute.PermissionUtils;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.Returns;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
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

    @Test
    public void testNoTargetFile() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        HttpConnectionReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError(anyString());
    }

    @Test
    public void testNoNetwork() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError(anyString());
    }

    @Test
    public void testNoExternalStoragePermission() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(false);
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError(anyString());
    }

    @Test
    public void testDownloadStart() throws Exception {
        ReleaseDetails mockReleaseDetails = mockTargetFile();
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString())).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener));
        doNothing().when(releaseDownloader).showProgressNotification(anyLong(), anyLong());
        releaseDownloader.resume();
        verify(mockListener).onStart(anyLong());
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

}
