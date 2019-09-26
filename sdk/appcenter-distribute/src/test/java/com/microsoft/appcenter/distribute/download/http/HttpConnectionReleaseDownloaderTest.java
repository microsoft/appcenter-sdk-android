/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.app.DownloadManager;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.stubbing.answers.Returns;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({SharedPreferencesManager.class, PermissionUtils.class, NetworkStateHelper.class, AsyncTaskUtils.class, DownloadManager.Request.class, Uri.class, Build.VERSION.class})
@RunWith(PowerMockRunner.class)
public class HttpConnectionReleaseDownloaderTest {

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
    public void testNoNetwork() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE)).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError(anyString());
    }

    @Test
    public void testNoExternalStoragePermission() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE)).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(false);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        ReleaseDownloader releaseDownloader = new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verify(mockListener).onError(anyString());
    }

    @Test
    public void testNormal() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE)).thenReturn(null);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        HttpConnectionReleaseDownloader releaseDownloader = spy(new HttpConnectionReleaseDownloader(mockContext, mockReleaseDetails, mockListener));
        doNothing().when(releaseDownloader).showProgressNotification(anyLong(), anyLong());
        releaseDownloader.resume();
        verify(mockListener).onStart(anyLong());
    }

}
