/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import com.microsoft.appcenter.distribute.ReleaseDetails;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.whenNew;


@PrepareForTest({
        DownloadManagerRequestTask.class,
        AsyncTask.class
})
@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerRequestTaskTest {

    private static final long DOWNLOAD_ID = 42;

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    private ReleaseDetails mReleaseDetails;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private DownloadManager.Request mDownloadManagerRequest;

    @Mock
    private DownloadManagerReleaseDownloader mDownloader;

    @Mock
    private Handler mHandler;

    @Mock
    private Cursor mCursor;

    private DownloadManagerRequestTask mRequestTask;

    @Before
    public void setUp() throws Exception {

        /* Mock Handler. */
        whenNew(Handler.class).withAnyArguments().thenReturn(mHandler);

        /* Mock DownloadManager. */
        when(mDownloadManager.enqueue(eq(mDownloadManagerRequest))).thenReturn(DOWNLOAD_ID);

        /* Mock Downloader. */
        when(mReleaseDetails.getDownloadUrl()).thenReturn(mock(Uri.class));
        when(mDownloader.getReleaseDetails()).thenReturn(mReleaseDetails);
        when(mDownloader.getDownloadManager()).thenReturn(mDownloadManager);

        /* Create RequestTask. */
        mRequestTask = spy(new DownloadManagerRequestTask(mDownloader, "title %1$s (%2$d)"));
        when(mRequestTask.createRequest(any(Uri.class))).thenReturn(mDownloadManagerRequest);
    }

    @Test
    public void downloadStarted() {
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(mReleaseDetails.getShortVersion()).thenReturn("1");
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        String expectedTitle = "title 1 (1)";
        verify(mDownloadManagerRequest).setTitle(eq(expectedTitle));
        verifyNoMoreInteractions(mDownloadManagerRequest);
        verify(mDownloader).onDownloadStarted(eq(DOWNLOAD_ID), anyLong());
    }

    @Test
    public void hideNotificationOnMandatoryUpdate() {
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(mReleaseDetails.getShortVersion()).thenReturn("1");
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        String expectedTitle = "title 1 (1)";
        verify(mDownloadManagerRequest).setTitle(eq(expectedTitle));
        verify(mDownloadManagerRequest).setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        verify(mDownloadManagerRequest).setVisibleInDownloadsUi(false);
        verify(mDownloader).onDownloadStarted(eq(DOWNLOAD_ID), anyLong());
    }

    @Test
    public void cancelledDuringEnqueue() {
        mRequestTask = spy(mRequestTask);
        when(mRequestTask.isCancelled()).thenReturn(true);

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        verify(mDownloader, never()).onDownloadStarted(anyLong(), anyLong());
    }

    @Test
    public void enqueueTaskIllegalStateExceptionHandled() {

        /* Mock DownloadManager. */
        when(mDownloadManager.enqueue(eq(mDownloadManagerRequest))).thenThrow(new IllegalArgumentException("MOCK Unknown URL content://downloads/my_download"));

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        verify(mDownloader).onDownloadError(any(IllegalStateException.class));
        verify(mDownloader, never()).onDownloadStarted(anyLong(), anyLong());
    }

    @Test
    public void noExceptionsWhenDownloadFinishedAfterTimeout() {
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mCursor);
        when(mCursor.moveToFirst()).thenReturn(false);
        when(mRequestTask.isCancelled()).thenReturn(false);

        /* Run Callback immediately. */
        when(mHandler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return true;
            }
        });

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        verify(mDownloader, never()).onDownloadError(any(IllegalStateException.class));
    }


    @Test
    public void throwExceptionWhenDownloadStillNotFinishedAfterTimeout() {
        when(mDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mCursor);
        when(mCursor.moveToFirst()).thenReturn(true);
        when(mRequestTask.isCancelled()).thenReturn(false);

        /* Run Callback immediately. */
        when(mHandler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return true;
            }
        });

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify that exception was thrown. */
        verify(mDownloader).onDownloadError(any(IllegalStateException.class));
    }

    @Test
    public void oldCallbacksRemovedBeforeCreatingNewDownload() {
        when(mRequestTask.isCancelled()).thenReturn(false);
        when(mRequestTask.createRequest(any(Uri.class))).thenCallRealMethod();

        /* Perform background task. Emulates creating callback, which would not be used */
        mRequestTask.doInBackground();

        /* Perform background task. Must delete old callbacks */
        mRequestTask.doInBackground();

        /* Verify that callback was removed. */
        verify(mHandler).removeCallbacks(any(Runnable.class));
    }
}
