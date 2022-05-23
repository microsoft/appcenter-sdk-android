/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.DownloadManager;
import android.database.Cursor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({
        DownloadManagerCancelPendingTask.class
})
public class DownloadManagerCancelPendingTaskTest {

    private static final long DOWNLOAD_ID = 42;

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    private DownloadManagerReleaseDownloader mDownloader;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private Cursor mCursor;

    private DownloadManagerCancelPendingTask mCancelPendingTask;

    @Before
    public void setUp() throws Exception {

        /* Mock DownloadManager. */
        when(mDownloader.getDownloadManager()).thenReturn(mDownloadManager);

        /* Mock query. */
        DownloadManager.Query query = mock(DownloadManager.Query.class);
        whenNew(DownloadManager.Query.class).withAnyArguments().thenReturn(query);
        when(query.setFilterById(anyLong())).thenReturn(query);
        when(query.setFilterByStatus(anyInt())).thenReturn(query);

        /* Create DownloadManagerCancelPendingTask. */
        mCancelPendingTask = new DownloadManagerCancelPendingTask(mDownloader, DOWNLOAD_ID);
    }

    @Test
    public void ifTaskIsPending() {
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mCursor);
        when(mCursor.moveToFirst()).thenReturn(true);

        /* Perform background task. */
        mCancelPendingTask.doInBackground();

        /* Verify. */
        verify(mCursor).moveToFirst();
        verify(mCursor).close();
        verifyNoMoreInteractions(mCursor);
        verify(mDownloader).clearDownloadId(DOWNLOAD_ID);
        verify(mDownloader).onDownloadError(any(RuntimeException.class));
    }

    @Test
    public void ifTaskIsNotPending() {
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mCursor);
        when(mCursor.moveToFirst()).thenReturn(false);

        /* Perform background task. */
        mCancelPendingTask.doInBackground();

        /* Verify. */
        verify(mCursor).moveToFirst();
        verify(mCursor).close();
        verifyNoMoreInteractions(mCursor);
        verify(mDownloader, never()).clearDownloadId(DOWNLOAD_ID);
        verify(mDownloader, never()).onDownloadError(any(RuntimeException.class));
    }

    @Test
    public void ifCursorIsNull() {
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(null);

        /* Perform background task. */
        mCancelPendingTask.doInBackground();

        /* Verify. */
        verifyNoMoreInteractions(mCursor);
        verify(mDownloader, never()).clearDownloadId(DOWNLOAD_ID);
        verify(mDownloader, never()).onDownloadError(any(RuntimeException.class));
    }
}
