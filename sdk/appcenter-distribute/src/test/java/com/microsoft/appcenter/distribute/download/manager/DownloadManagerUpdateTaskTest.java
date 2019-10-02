/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerUpdateTaskTest {

    private static final long DOWNLOAD_ID = 42;

    @Mock
    private Cursor mCursor;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private DownloadManagerReleaseDownloader mDownloader;

    private DownloadManagerUpdateTask mUpdateTask;

    @Before
    public void setUp() {

        /* Mock Cursor. */
        when(mCursor.moveToFirst()).thenReturn(true);
        when(mCursor.getColumnIndexOrThrow(eq(DownloadManager.COLUMN_STATUS))).thenReturn(DownloadManager.COLUMN_STATUS.hashCode());

        /* Mock DownloadManager. */
        when(mDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(DOWNLOAD_ID);
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mCursor);

        /* Mock Downloader. */
        when(mDownloader.getDownloadManager()).thenReturn(mDownloadManager);
        when(mDownloader.getDownloadId()).thenReturn(DOWNLOAD_ID);

        /* Create UpdateTask. */
        mUpdateTask = new DownloadManagerUpdateTask(mDownloader);
    }

    @Test
    public void downloadIdIsInvalid() {
        when(mDownloader.getDownloadId()).thenReturn(INVALID_DOWNLOAD_IDENTIFIER);

        /* Perform background task. */
        mUpdateTask.doInBackground(null);

        /* Verify. */
        verify(mDownloader).onStart();
    }

    @Test
    public void errorOnNullCursor() {
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(null);

        /* Perform background task. */
        mUpdateTask.doInBackground(null);

        /* Verify. */
        verify(mDownloader).onDownloadError(any(RuntimeException.class));
    }

    @Test
    public void errorOnEmptyCursor() {
        when(mCursor.moveToFirst()).thenReturn(false);

        /* Perform background task. */
        mUpdateTask.doInBackground(null);

        /* Verify. */
        verify(mDownloader).onDownloadError(any(RuntimeException.class));
        verify(mCursor).close();
    }

    @Test
    public void doNothingAfterCancellation() {
        mUpdateTask = spy(mUpdateTask);
        when(mUpdateTask.isCancelled()).thenReturn(true);

        /* Perform background task. */
        mUpdateTask.doInBackground(null);

        /* Verify. */
        verifyZeroInteractions(ignoreStubs(mDownloader));
    }

    @Test
    public void errorOnFailedStatus() {
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_FAILED);

        /* Perform background task. */
        mUpdateTask.doInBackground(null);

        /* Verify. */
        verify(mDownloader).onDownloadError(any(RuntimeException.class));
        verify(mCursor).close();
    }

    @Test
    public void reportProgress() {
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_RUNNING);

        /* Perform background task. */
        mUpdateTask.doInBackground(null);

        /* Verify. */
        verify(mCursor).close();
    }

    @Test
    public void completeDownloading() {
        when(mCursor.getInt(eq(DownloadManager.COLUMN_STATUS.hashCode()))).thenReturn(DownloadManager.STATUS_SUCCESSFUL);

        /* Perform background task. */
        mUpdateTask.doInBackground(null);

        /* Verify. */
        verify(mDownloader).onDownloadComplete(eq(mCursor));
        verify(mDownloader, never()).onDownloadError(any(RuntimeException.class));
        verify(mCursor).close();
    }
}
