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

    private static final long WRONG_DOWNLOAD_ID = 123;

    @Mock
    private Context mContext;

    @Mock
    private ReleaseDownloader.Listener mListener;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private ReleaseDetails mReleaseDetails;

    @Mock
    private DownloadManagerUpdateTask mUpdateTask;

    @Mock
    private DownloadManagerRequestTask mRequestTask;

    @Mock
    private DownloadManagerRemoveTask mRemoveTask;

    @Mock
    private Cursor mCursor;

    private DownloadManagerReleaseDownloader mReleaseDownloader;

    @Before
    public void setUp() {
        mockStatic(SharedPreferencesManager.class);
        mockStatic(HandlerUtils.class);

        /* Mock Uri. */
        mockStatic(Uri.class);
        when(Uri.parse(anyString())).thenReturn(mock(Uri.class));
        when(mReleaseDetails.getDownloadUrl()).thenReturn(mock(Uri.class));

        /* Mock DownloadManager. */
        when(mContext.getSystemService(DOWNLOAD_SERVICE)).thenReturn(mDownloadManager);
        when(mDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(DOWNLOAD_ID);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mCursor);

        /* Mock AsyncTaskUtils. */
        mockStatic(AsyncTaskUtils.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).thenReturn(mUpdateTask);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).thenReturn(mRequestTask);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class))).thenReturn(mRemoveTask);

        /* Mock Cursor. */
        when(mCursor.moveToFirst()).thenReturn(true);
        when(mCursor.getColumnIndexOrThrow(eq(DownloadManager.COLUMN_STATUS))).thenReturn(DownloadManager.COLUMN_STATUS.hashCode());
        when(mCursor.getColumnIndexOrThrow(eq(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))).thenReturn(DownloadManager.COLUMN_TOTAL_SIZE_BYTES.hashCode());
        when(mCursor.getColumnIndexOrThrow(eq(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))).thenReturn(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR.hashCode());
        when(mCursor.getColumnIndexOrThrow(eq(DownloadManager.COLUMN_LOCAL_URI))).thenReturn(DownloadManager.COLUMN_LOCAL_URI.hashCode());
        when(mCursor.getLong(eq(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR.hashCode()))).thenReturn(42L);
        when(mCursor.getLong(eq(DownloadManager.COLUMN_TOTAL_SIZE_BYTES.hashCode()))).thenReturn(4242L);

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
    public void cancelClearsEverything() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER)
                .thenReturn(DOWNLOAD_ID);

        /* Download isn't started yet. */
        assertFalse(mReleaseDownloader.isDownloading());

        /* Create request. */
        mReleaseDownloader.resume();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());

        /* Update status. */
        mReleaseDownloader.resume();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());

        /* Download is already started. */
        assertTrue(mReleaseDownloader.isDownloading());

        /* Cancel clears everything on. */
        mReleaseDownloader.cancel();
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
    public void startDownloadingMandatoryUpdate() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* Request new downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onRequest(mRequestTask);

        /* Verify. */
        verify(mListener).onStart(anyLong());
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void startDownloadingNotMandatoryUpdate() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* Request new downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onRequest(mRequestTask);

        /* Verify. */
        verify(mListener).onStart(anyLong());
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void startDownloadingRemovesUnexpectedDownloadId() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER)
                .thenReturn(WRONG_DOWNLOAD_ID);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);
        assertEquals(mReleaseDetails, mReleaseDownloader.getReleaseDetails());

        /* Request new downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onRequest(mRequestTask);

        /* Verify. */
        verify(mDownloadManager).remove(eq(WRONG_DOWNLOAD_ID));
        verify(mListener).onStart(anyLong());
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void startDownloadingFromUnexpectedAsyncTask() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(INVALID_DOWNLOAD_IDENTIFIER);

        /* Request new downloading, but call onRequest from another async task. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onRequest(mock(DownloadManagerRequestTask.class));

        /* Verify. */
        verify(mListener, never()).onStart(anyLong());
        verifyStatic(never());
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(DOWNLOAD_ID));
        verify(mDownloadManager).remove(eq(DOWNLOAD_ID));
    }

    @Test
    public void errorOnNullCursor() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(null);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mListener).onError(anyString());
    }

    @Test
    public void errorOnEmptyCursor() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mCursor.moveToFirst()).thenReturn(false);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mListener).onError(anyString());
        verify(mCursor).close();
    }

    @Test
    public void errorOnFailedStatus() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_FAILED);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mListener).onError(anyString());
        verify(mCursor).close();
    }

    @Test
    public void scheduleAnotherUpdateIfListenerWantsIt() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_RUNNING);
        when(mListener.onProgress(anyInt(), anyInt())).thenReturn(true);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic(times(2));
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mCursor).close();
    }

    @Test
    public void doNotScheduleAnotherUpdateIfListenerDoNotWantsIt() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_RUNNING);
        when(mListener.onProgress(anyInt(), anyInt())).thenReturn(false);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mCursor).close();
    }

    @Test
    public void completeDownloading() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        when(mListener.onComplete(any(Uri.class))).thenReturn(true);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mListener).onComplete(any(Uri.class));
        verify(mListener, never()).onError(anyString());
        verify(mCursor).close();
    }

    @Test
    public void completeDownloadingFallbackOnOldDevices() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        when(mListener.onComplete(any(Uri.class))).thenReturn(false).thenReturn(true);
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mListener, times(2)).onComplete(any(Uri.class));
        verify(mListener, never()).onError(anyString());
        verify(mCursor).close();
    }

    @Test
    public void completeDownloadingFallbackOnNewDevices() {
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER)))
                .thenReturn(DOWNLOAD_ID);
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        when(mListener.onComplete(any(Uri.class))).thenReturn(false).thenReturn(true);
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.O);

        /* Resume downloading. */
        mReleaseDownloader.resume();
        mReleaseDownloader.onUpdate();

        /* Verify. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mListener).onComplete(any(Uri.class));
        verify(mListener).onError(anyString());
        verify(mCursor).close();
    }
}
