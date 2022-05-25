/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileNotFoundException;
import java.io.IOException;

@PrepareForTest({
        AsyncTaskUtils.class,
        Build.VERSION.class,
        DownloadManager.Request.class,
        HandlerUtils.class,
        SharedPreferencesManager.class,
        Uri.class
})
@RunWith(PowerMockRunner.class)
public class DownloadManagerReleaseDownloaderTest {

    private static final long DOWNLOAD_ID = 42;

    private static final long PACKAGE_SIZE = 42 * 1024;

    @Mock
    private Context mContext;

    @Mock
    private ReleaseDetails mReleaseDetails;

    @Mock
    private ReleaseDownloader.Listener mListener;

    @Mock
    private DownloadManagerUpdateTask mUpdateTask;

    @Mock
    private DownloadManagerRequestTask mRequestTask;

    @Mock
    private DownloadManagerRemoveTask mRemoveTask;

    @Mock
    private Handler mMainHandler;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private ParcelFileDescriptor mFileDescriptor;

    private DownloadManagerReleaseDownloader mReleaseDownloader;

    @Before
    public void setUp() throws Exception {
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);

        /* Mock AsyncTaskUtils. */
        mockStatic(AsyncTaskUtils.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).thenReturn(mUpdateTask);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).thenReturn(mRequestTask);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class))).thenReturn(mRemoveTask);

        /* Run handler immediately. */
        mockStatic(HandlerUtils.class);
        when(HandlerUtils.getMainHandler()).thenReturn(mMainHandler);
        when(mMainHandler.postAtTime(any(Runnable.class), anyString(), anyLong())).thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return null;
            }
        });

        /* Mock download manager. */
        when(mContext.getSystemService(DOWNLOAD_SERVICE)).thenReturn(mDownloadManager);
        when(mDownloadManager.getUriForDownloadedFile(anyLong())).thenReturn(mock(Uri.class));

        /* Mock package size. */
        when(mFileDescriptor.getStatSize()).thenReturn(PACKAGE_SIZE);
        when(mDownloadManager.openDownloadedFile(anyLong())).thenReturn(mFileDescriptor);
        when(mReleaseDetails.getSize()).thenReturn(PACKAGE_SIZE);

        /* Create release downloader. */
        mReleaseDownloader = new DownloadManagerReleaseDownloader(mContext, mReleaseDetails, mListener);
    }

    @Test
    public void getDownloadManager() {
        DownloadManager downloadManager = mock(DownloadManager.class);
        when(mContext.getSystemService(DOWNLOAD_SERVICE)).thenReturn(downloadManager);
        assertEquals(downloadManager, mReleaseDownloader.getDownloadManager());
    }

    @Test
    public void downloadStatus() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER)
                .thenReturn(DOWNLOAD_ID)
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
        assertFalse(mReleaseDownloader.isDownloading());
        assertEquals(INVALID_DOWNLOAD_IDENTIFIER, mReleaseDownloader.getDownloadId());
        assertEquals(DOWNLOAD_ID, mReleaseDownloader.getDownloadId());
        assertEquals(DOWNLOAD_ID, mReleaseDownloader.getDownloadId());
        assertTrue(mReleaseDownloader.isDownloading());
        verifyStatic(SharedPreferencesManager.class, times(2));
        SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER));
    }

    @Test
    public void resumeStartsUpdateTask() {
        mReleaseDownloader.resume();
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), any());
    }

    @Test
    public void cancelClearsEverything() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID)
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);

        /* Update status. */
        mReleaseDownloader.resume();
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), any());

        /* Create new request. */
        mReleaseDownloader.onStart();
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), any());

        /* Cancel clears everything only once. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.cancel();

        /* Verify. */
        verify(mRequestTask).cancel(eq(true));
        verify(mUpdateTask).cancel(eq(true));
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class), any());
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void clearDownloadId() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        assertEquals(DOWNLOAD_ID, mReleaseDownloader.getDownloadId());

        /* Clear with valid id. */
        mReleaseDownloader.clearDownloadId(DOWNLOAD_ID);

        /* Verify. */
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void clearInvalidDownloadId() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        assertEquals(DOWNLOAD_ID, mReleaseDownloader.getDownloadId());

        /* Clear with invalid id. */
        mReleaseDownloader.clearDownloadId(43);

        /* Verify. */
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class));
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void cancelPendingDownloadIfWasCancelled() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);

        /* Start download. */
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);

        /* Capture timeout callback. */
        ArgumentCaptor<Runnable> timeoutCallback = ArgumentCaptor.forClass(Runnable.class);
        verify(mMainHandler).postDelayed(timeoutCallback.capture(), anyLong());

        /* Cancel before timeout. */
        mReleaseDownloader.cancel();
        timeoutCallback.getValue().run();

        /* Verify. */
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerCancelPendingTask.class));
    }

    @Test
    public void cancelPendingDownloadIfWasNotCancelled() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);

        /* Start download. */
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);

        /* Capture timeout callback. */
        ArgumentCaptor<Runnable> timeoutCallback = ArgumentCaptor.forClass(Runnable.class);
        verify(mMainHandler).postDelayed(timeoutCallback.capture(), anyLong());

        /* Call timeout when download is in progress. */
        timeoutCallback.getValue().run();

        /* Verify. */
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerCancelPendingTask.class));
    }

    @Test
    public void doNotTryToRemoveInvalidDownloadId() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
        mReleaseDownloader.cancel();

        /* Verify. */
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class), any());
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void requestNewDownloading() {

        /* Create new request. */
        mReleaseDownloader.onStart();
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), any());

        /* Do not duplicate requests. */
        mReleaseDownloader.onStart();
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), any());

        /* Don't do anything after cancellation. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.onStart();
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), any());
    }

    @Test
    public void doNotRequestNewDownloadingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.onStart();
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), any());
    }

    @Test
    public void startDownloadingMandatoryUpdate() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* When download request has been enqueued. */
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);

        /* Verify. */
        verify(mListener).onStart(anyLong());
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), any());
    }

    @Test
    public void startDownloadingNotMandatoryUpdate() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* When download request has been enqueued. */
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);

        /* Verify. */
        verify(mListener).onStart(anyLong());
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), any());
    }

    @Test
    public void doNotStartDownloadingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);
        verify(mListener, never()).onStart(anyLong());
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
    }

    @Test
    public void scheduleAnotherUpdateIfListenerWantsIt() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getLong(eq(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR.hashCode()))).thenReturn(42L);
        when(cursor.getLong(eq(DownloadManager.COLUMN_TOTAL_SIZE_BYTES.hashCode()))).thenReturn(4242L);
        when(mListener.onProgress(anyLong(), anyLong())).thenReturn(true);

        /* Update download progress. */
        mReleaseDownloader.onDownloadProgress(cursor);

        /* Verify. */
        verify(mListener).onProgress(anyLong(), anyLong());
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), any());
    }

    @Test
    public void doNotScheduleAnotherUpdateIfListenerDoNotWantsIt() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getLong(eq(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR.hashCode()))).thenReturn(42L);
        when(cursor.getLong(eq(DownloadManager.COLUMN_TOTAL_SIZE_BYTES.hashCode()))).thenReturn(4242L);
        when(mListener.onProgress(anyLong(), anyLong())).thenReturn(false);

        /* Update download progress. */
        mReleaseDownloader.onDownloadProgress(cursor);

        /* Verify. */
        verify(mListener).onProgress(anyLong(), anyLong());
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), any());
    }

    @Test
    public void doNotOnDownloadProgressAfterCancellation() {

        /* Update download progress. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadProgress(mock(Cursor.class));

        /* Verify. */
        verify(mListener, never()).onProgress(anyLong(), anyLong());
    }

    @Test
    public void completeDownload() throws IOException {

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mFileDescriptor).close();
        verify(mListener).onComplete(any(Uri.class));
        verify(mListener, never()).onError(anyString());
    }

    @Test
    public void completeDownloadDoesNothingAfterCancellation() {
        mockStatic(Uri.class);
        when(Uri.parse(anyString())).thenReturn(mock(Uri.class));

        /* Complete download after cancelling. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mListener, never()).onComplete(any(Uri.class));
    }

    @Test
    public void errorDownload() {
        mReleaseDownloader.onDownloadError(new RuntimeException("Test"));

        /* Verify. */
        verify(mListener).onError(anyString());
    }

    @Test
    public void errorOnOpenDownloadedFile() throws IOException {

        /* Throw exception. */
        when(mDownloadManager.openDownloadedFile(anyLong())).thenThrow(new FileNotFoundException());

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mListener).onError(anyString());
    }

    @Test
    public void errorOnInvalidFile() throws IOException {

        /* If size is different. */
        when(mReleaseDetails.getSize()).thenReturn(142 * 1024L);

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mFileDescriptor).close();
        verify(mListener).onError(anyString());
    }

    @Test
    public void errorDownloadFileNotFound() throws IOException {

        /* DownloadManager returns null. */
        when(mDownloadManager.getUriForDownloadedFile(anyLong())).thenReturn(null);

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mFileDescriptor).close();
        verify(mListener).onError(anyString());
    }

    @Test
    public void exceptionOnClosingFileDescriptor() throws IOException {

        /* Throw exception in invalid size callback. */
        doThrow(new IOException()).when(mFileDescriptor).close();

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mFileDescriptor).close();
        verify(mListener, never()).onComplete(any(Uri.class));
        verify(mListener).onError(anyString());
    }

    @Test
    public void errorDownloadDoesNothingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadError(new RuntimeException("Test"));

        /* Verify. */
        verify(mListener, never()).onError(anyString());
    }
}
