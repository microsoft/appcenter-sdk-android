/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.microsoft.appcenter.distribute.ReleaseDetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({
        Handler.class,
        Looper.class
})
@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerRequestTaskTest {

    private static final long DOWNLOAD_ID = 42;

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

    private DownloadManagerRequestTask mRequestTask;

    @Before
    public void setUp() throws Exception {

        when(mHandler.postDelayed(Matchers.<Runnable>any(), anyLong())).thenReturn(false);
        PowerMockito.whenNew(Handler.class).withArguments(Looper.class).thenReturn(mHandler);


        /* Mock DownloadManager. */
        when(mDownloadManager.enqueue(eq(mDownloadManagerRequest))).thenReturn(DOWNLOAD_ID);

        /* Mock Downloader. */
        when(mDownloader.getReleaseDetails()).thenReturn(mReleaseDetails);
        when(mDownloader.getDownloadManager()).thenReturn(mDownloadManager);

        new DownloadManagerRequestTask(mDownloader, "title %1$s (%2$d)");

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
        verifyZeroInteractions(mDownloadManagerRequest);
        verify(mDownloader).onDownloadStarted(eq(DOWNLOAD_ID), anyLong());
    }

    @Test
    public void hideNotificationOnMandatoryUpdate() {
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(mReleaseDetails.getShortVersion()).thenReturn("1");
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);
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
    public void handlerCreated() throws Exception {
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(mReleaseDetails.getShortVersion()).thenReturn("1");
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);

//        mHandler = new Handler(Looper.getMainLooper());
//        when(mHandler.po///postDelayed(Matchers.<Runnable>any(), anyLong())).thenReturn(false);
//        PowerMockito.whenNew(Handler.class).withAnyArguments().thenReturn(mHandler);

        /* Perform background task. */
        mRequestTask.doInBackground();


    }
}
