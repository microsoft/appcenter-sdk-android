/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
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
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({
        AsyncTaskUtils.class,
        Build.VERSION.class,
        NetworkStateHelper.class,
        NotificationManager.class,
        PermissionUtils.class,
        SharedPreferencesManager.class,
        Uri.class
})
@RunWith(PowerMockRunner.class)
public class HttpConnectionReleaseDownloaderTest {

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Mock
    private Context mContext;

    @Mock
    private ReleaseDetails mReleaseDetails;

    @Mock
    private ReleaseDownloader.Listener mListener;

    @Mock
    private NetworkStateHelper mNetworkStateHelper;

    @Mock
    private HttpConnectionCheckTask mCheckTask;

    @Mock
    private HttpConnectionDownloadFileTask mDownloadFileTask;

    @Mock
    private HttpConnectionRemoveFileTask mRemoveFileTask;

    @Mock
    private NotificationManager mNotificationManager;

    private HttpConnectionReleaseDownloader mReleaseDownloader;

    @Before
    public void setUp() {
        mockStatic(SharedPreferencesManager.class);

        /* Mock Context. */
        when(mContext.getExternalFilesDir(anyString())).thenReturn(mTemporaryFolder.getRoot());
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManager);
        when(mContext.getApplicationInfo()).thenReturn(mock(ApplicationInfo.class));

        /* Mock AsyncTaskUtils. */
        mockStatic(AsyncTaskUtils.class);
        when(AsyncTaskUtils.execute(anyString(), isA(HttpConnectionCheckTask.class))).thenReturn(mCheckTask);
        when(AsyncTaskUtils.execute(anyString(), isA(HttpConnectionDownloadFileTask.class))).thenReturn(mDownloadFileTask);
        when(AsyncTaskUtils.execute(anyString(), isA(HttpConnectionRemoveFileTask.class))).thenReturn(mRemoveFileTask);

        /* Mock NetworkStateHelper. */
        mockStatic(NetworkStateHelper.class);
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(mNetworkStateHelper);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Mock PermissionUtils. */
        mockStatic(PermissionUtils.class);
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(true);

        /* Create ReleaseDownloader. */
        mReleaseDownloader = new HttpConnectionReleaseDownloader(mContext, mReleaseDetails, mListener);
    }

    @Test
    public void getTargetFile() {
        assertNotNull(mReleaseDownloader.getTargetFile());

        /* Second call uses cached value. */
        assertNotNull(mReleaseDownloader.getTargetFile());
        verify(mContext).getExternalFilesDir(anyString());
    }

    @Test
    public void getNotificationBuilder() {

        /* Second call uses cached value. */
        assertEquals(mReleaseDownloader.getNotificationBuilder(), mReleaseDownloader.getNotificationBuilder());
    }

    @Test
    public void downloadDirectoryNotAvailable() {
        when(mContext.getExternalFilesDir(anyString())).thenReturn(null);
        assertNull(mReleaseDownloader.getTargetFile());
    }

    @Test
    public void resumeStartsCheck() {

        /* Resume download. */
        mReleaseDownloader.resume();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionCheckTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void resumeWithoutNetwork() {
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);

        /* Resume download. */
        mReleaseDownloader.resume();

        /* Verify an error. */
        verify(mListener).onError(anyString());
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionCheckTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void resumeWithoutPermissions() {
        when(PermissionUtils.permissionsAreGranted(any(int[].class))).thenReturn(false);

        /* Resume download. */
        mReleaseDownloader.resume();

        /* Verify an error. */
        verify(mListener).onError(anyString());
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionCheckTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void resumeDoesNothingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.resume();
        verifyZeroInteractions(mListener);
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionCheckTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void resumeDoesNotCheckingStatusTwice() {
        mReleaseDownloader.resume();
        mReleaseDownloader.resume();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionCheckTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void cancelClearsEverything() {
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), anyString()))
                .thenReturn("/mock/file");

        /* Check status. */
        mReleaseDownloader.resume();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionCheckTask.class), Mockito.<Void>anyVararg());

        /* Start new download. */
        mReleaseDownloader.onStart(mock(File.class));
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionDownloadFileTask.class), Mockito.<Void>anyVararg());

        /* Cancel clears everything only once. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.cancel();

        /* Verify. */
        verify(mCheckTask).cancel(eq(true));
        verify(mDownloadFileTask).cancel(eq(true));
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionRemoveFileTask.class), Mockito.<Void>anyVararg());
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE));
        verify(mNotificationManager).cancel(anyInt());
    }

    @Test
    public void startNewDownload() {

        /* Start new download. */
        assertFalse(mReleaseDownloader.isDownloading());
        mReleaseDownloader.onStart(mock(File.class));
        assertTrue(mReleaseDownloader.isDownloading());

        /* Duplicate doesn't have any effect. */
        mReleaseDownloader.onStart(mock(File.class));

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionDownloadFileTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void startNewDownloadDoesNothingAfterCancellation() {

        /* Start new download after cancellation. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.onStart(mock(File.class));

        /* Verify. */
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(HttpConnectionDownloadFileTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void showProgressNotificationOnDownloadStart() {
        mReleaseDownloader = spy(mReleaseDownloader);
        when(mReleaseDownloader.getNotificationBuilder())
                .thenReturn(mock(Notification.Builder.class, RETURNS_MOCKS));

        /* When download has been started. */
        mReleaseDownloader.onDownloadStarted(0);

        /* Verify. */
        verify(mNotificationManager).notify(anyInt(), any(Notification.class));
        verify(mListener).onStart(anyLong());
    }

    @Test
    public void doNotShowProgressNotificationForMandatoryRelease() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* When download has been started. */
        mReleaseDownloader.onDownloadStarted(0);

        /* Verify. */
        verify(mNotificationManager, never()).notify(anyInt(), any(Notification.class));
        verify(mListener).onStart(anyLong());
    }

    @Test
    public void doNothingOnDownloadStartedAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadStarted(0);
        verify(mNotificationManager, never()).notify(anyInt(), any(Notification.class));
        verify(mListener, never()).onStart(anyLong());
    }

    @Test
    public void onDownloadProgress() {
        mReleaseDownloader = spy(mReleaseDownloader);
        when(mReleaseDownloader.getNotificationBuilder())
                .thenReturn(mock(Notification.Builder.class, RETURNS_MOCKS));

        /* Update download progress. */
        mReleaseDownloader.onDownloadProgress(1, 2);

        /* Verify. */
        verify(mNotificationManager).notify(anyInt(), any(Notification.class));
        verify(mListener).onProgress(eq(1L), eq(2L));

        /* Update download progress one more time. */
        mReleaseDownloader.onDownloadProgress(2, 2);

        /* Verify. */
        verify(mNotificationManager, times(2)).notify(anyInt(), any(Notification.class));
        verify(mListener).onProgress(eq(2L), eq(2L));
    }

    @Test
    public void doNotOnDownloadProgressAfterCancellation() {

        /* Update download progress. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadProgress(1, 2);

        /* Verify. */
        verify(mNotificationManager, never()).notify(anyInt(), any(Notification.class));
        verify(mListener, never()).onProgress(anyInt(), anyInt());
    }

    @Test
    public void downloadComplete() throws IOException {
        mockStatic(Uri.class);
        when(Uri.parse(anyString())).thenReturn(mock(Uri.class));
        File targetFile = mTemporaryFolder.newFile();
        Files.write(Paths.get(targetFile.toURI()), "I'm an APK file".getBytes());
        when(mReleaseDetails.getSize()).thenReturn(targetFile.length());

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete(targetFile);

        /* Verify. */
        verify(mNotificationManager).cancel(anyInt());
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE), eq(targetFile.getAbsolutePath()));
        verifyStatic();
        Uri.parse(eq("file://" + targetFile.getAbsolutePath()));
        verify(mListener).onComplete(any(Uri.class));
    }

    @Test
    public void downloadCompleteWithWrongFile() throws IOException {
        File targetFile = mTemporaryFolder.newFile();
        Files.write(Paths.get(targetFile.toURI()), "I'm an APK file".getBytes());
        when(mReleaseDetails.getSize()).thenReturn(42L);

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete(targetFile);

        /* Verify. */
        verify(mNotificationManager).cancel(anyInt());
        verifyStatic(never());
        SharedPreferencesManager.putString(anyString(), anyString());
        verify(mListener, never()).onComplete(any(Uri.class));
    }

    @Test
    public void downloadCompleteDoesNothingAfterCancellation() {

        /* Cancel. */
        mReleaseDownloader.cancel();
        verify(mNotificationManager).cancel(anyInt());
        reset(mNotificationManager);

        /* Complete download after cancellation. */
        mReleaseDownloader.onDownloadComplete(mock(File.class));

        /* Verify. */
        verifyZeroInteractions(mNotificationManager, mListener);
        verifyStatic(never());
        SharedPreferencesManager.putString(anyString(), anyString());
    }

    @Test
    public void downloadError() {
        mReleaseDownloader.onDownloadError("error");

        /* Verify. */
        verify(mNotificationManager).cancel(anyInt());
        verify(mListener).onError(anyString());
    }

    @Test
    public void downloadErrorDoesNothingAfterCancellation() {

        /* Cancel. */
        mReleaseDownloader.cancel();
        verify(mNotificationManager).cancel(anyInt());
        reset(mNotificationManager);

        /* Error download after cancellation. */
        mReleaseDownloader.onDownloadError("error");

        /* Verify. */
        verifyZeroInteractions(mNotificationManager, mListener);
    }

    @Test
    public void requiredPermissionsForOldAndroidVersions() {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN);
        assertArrayEquals(
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                HttpConnectionReleaseDownloader.requiredPermissions());
    }

    @Test
    public void requiredPermissionsForKitKat() {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.KITKAT);
        assertArrayEquals(new String[0], HttpConnectionReleaseDownloader.requiredPermissions());
    }
}
