/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

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

    private DownloadManagerReleaseDownloader mReleaseDownloader;

    @Before
    public void setUp() {
        mockStatic(SharedPreferencesManager.class);
        mockStatic(HandlerUtils.class);

        /* Mock AsyncTaskUtils. */
        mockStatic(AsyncTaskUtils.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).thenReturn(mUpdateTask);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).thenReturn(mRequestTask);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class))).thenReturn(mRemoveTask);

        /* Run handler immediately. */
        Handler handler = mock(Handler.class);
        when(HandlerUtils.getMainHandler()).thenReturn(handler);
        when(handler.postAtTime(any(Runnable.class), anyString(), anyLong())).thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

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
        verifyStatic(times(2));
        SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER));
    }

    @Test
    public void resumeStartsUpdateTask() {
        mReleaseDownloader.resume();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void resumeDoesNothingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.resume();
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void cancelClearsEverything() {

        /* Update status. */
        mReleaseDownloader.resume();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());

        /* Create new request. */
        mReleaseDownloader.onStart();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());

        /* Cancel clears everything only once. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.cancel();

        /* Verify. */
        verify(mRequestTask).cancel(eq(true));
        verify(mUpdateTask).cancel(eq(true));
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class), Mockito.<Void>anyVararg());
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void doNotTryToRemoveInvalidDownloadId() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
        mReleaseDownloader.cancel();

        /* Verify. */
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class), Mockito.<Void>anyVararg());
        verifyStatic(never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void requestNewDownloading() {

        /* Create new request. */
        mReleaseDownloader.onStart();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());

        /* Do not duplicate requests. */
        mReleaseDownloader.onStart();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());

        /* Don't do anything after cancellation. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.onStart();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void doNotRequestNewDownloadingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.onStart();
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void startDownloadingMandatoryUpdate() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* When download request has been enqueued. */
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);

        /* Verify. */
        verify(mListener).onStart(anyLong());
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void startDownloadingNotMandatoryUpdate() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* When download request has been enqueued. */
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);

        /* Verify. */
        verify(mListener).onStart(anyLong());
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void doNotStartDownloadingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadStarted(DOWNLOAD_ID, 0);
        verify(mListener, never()).onStart(anyLong());
        verifyStatic(never());
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
    }

    @Test
    public void scheduleAnotherUpdateIfListenerWantsIt() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getLong(eq(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR.hashCode()))).thenReturn(42L);
        when(cursor.getLong(eq(DownloadManager.COLUMN_TOTAL_SIZE_BYTES.hashCode()))).thenReturn(4242L);
        when(mListener.onProgress(anyInt(), anyInt())).thenReturn(true);

        /* Update download progress. */
        mReleaseDownloader.onDownloadProgress(cursor);

        /* Verify. */
        verify(mListener).onProgress(anyInt(), anyInt());
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void doNotScheduleAnotherUpdateIfListenerDoNotWantsIt() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getLong(eq(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR.hashCode()))).thenReturn(42L);
        when(cursor.getLong(eq(DownloadManager.COLUMN_TOTAL_SIZE_BYTES.hashCode()))).thenReturn(4242L);
        when(mListener.onProgress(anyInt(), anyInt())).thenReturn(false);

        /* Update download progress. */
        mReleaseDownloader.onDownloadProgress(cursor);

        /* Verify. */
        verify(mListener).onProgress(anyInt(), anyInt());
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void doNotOnDownloadProgressAfterCancellation() {

        /* Update download progress. */
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadProgress(mock(Cursor.class));

        /* Verify. */
        verify(mListener, never()).onProgress(anyInt(), anyInt());
    }

    @Test
    public void completeDownload() {
        mockStatic(Uri.class);
        when(Uri.parse(anyString())).thenReturn(mock(Uri.class));

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mListener).onComplete(anyLong());
        verify(mListener, never()).onError(anyString());
    }

    @Test
    public void completeDownloadFallbackOnOldDevices() {
        mockStatic(Uri.class);
        when(Uri.parse(anyString())).thenReturn(mock(Uri.class));
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);

        /* Complete download. */
        mReleaseDownloader.onDownloadComplete();

        /* Verify. */
        verify(mListener).onComplete(anyLong());
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
        verify(mListener, never()).onComplete(anyLong());
    }

    @Test
    public void errorDownload() {
        mReleaseDownloader.onDownloadError(mock(RuntimeException.class));

        /* Verify. */
        verify(mListener).onError(anyString());
    }

    @Test
    public void errorDownloadDoesNothingAfterCancellation() {
        mReleaseDownloader.cancel();
        mReleaseDownloader.onDownloadError(mock(RuntimeException.class));

        /* Verify. */
        verify(mListener, never()).onError(anyString());
    }
}
