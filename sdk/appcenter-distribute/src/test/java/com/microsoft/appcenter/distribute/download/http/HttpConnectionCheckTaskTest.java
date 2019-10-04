/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpConnectionCheckTaskTest {

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Mock
    private HttpConnectionReleaseDownloader mDownloader;

    private HttpConnectionCheckTask mCheckTask;

    @Before
    public void setUp() {
        mCheckTask = new HttpConnectionCheckTask(mDownloader);
    }

    @Test
    public void errorIfTargetFileIsNull() {
        when(mDownloader.getTargetFile()).thenReturn(null);

        /* Perform background task. */
        mCheckTask.doInBackground();

        /* Verify that onDownloadError callback is called. */
        verify(mDownloader).onDownloadError(anyString());
        verifyNoMoreInteractions(ignoreStubs(mDownloader));
    }

    @Test
    public void downloadedFileExists() throws Exception {
        File targetFile = mTemporaryFolder.newFile();
        when(mDownloader.getTargetFile()).thenReturn(targetFile);
        when(mDownloader.getDownloadedReleaseFilePath()).thenReturn(targetFile.getAbsolutePath());

        /* Perform background task. */
        mCheckTask.doInBackground();

        /* Verify that onDownloadComplete callback is called with the right target file. */
        verify(mDownloader).onDownloadComplete(targetFile);
        verifyNoMoreInteractions(ignoreStubs(mDownloader));
    }

    @Test
    public void downloadedFilePathDoesNotExist() {
        File targetFile = new File("/this/file/does/not/exist");
        when(mDownloader.getTargetFile()).thenReturn(targetFile);
        when(mDownloader.getDownloadedReleaseFilePath()).thenReturn(targetFile.getAbsolutePath());

        /* Perform background task. */
        mCheckTask.doInBackground();

        /* Verify that onStart is called. */
        verify(mDownloader).onStart(targetFile);
        verifyNoMoreInteractions(ignoreStubs(mDownloader));
    }

    @Test
    public void downloadedFileIsNotEqualToTargetFile() throws Exception {
        File downloadedFile = mTemporaryFolder.newFile();
        File targetFile = mTemporaryFolder.newFile();
        when(mDownloader.getTargetFile()).thenReturn(targetFile);
        when(mDownloader.getDownloadedReleaseFilePath()).thenReturn(downloadedFile.getAbsolutePath());

        /* Perform background task. */
        mCheckTask.doInBackground();

        /* Verify that the previous file is deleted. */
        assertFalse(downloadedFile.exists());
        verify(mDownloader).setDownloadedReleaseFilePath(isNull(String.class));

        /* Verify that onStart is called. */
        verify(mDownloader).onStart(targetFile);
        verifyNoMoreInteractions(ignoreStubs(mDownloader));
    }

    @Test
    public void downloadedFilePathIsNull() throws Exception {
        File targetFile = mTemporaryFolder.newFile();
        when(mDownloader.getTargetFile()).thenReturn(targetFile);
        when(mDownloader.getDownloadedReleaseFilePath()).thenReturn(null);

        /* Perform background task. */
        mCheckTask.doInBackground();

        /* Verify that onStart is called. */
        verify(mDownloader).onStart(targetFile);
        verifyNoMoreInteractions(ignoreStubs(mDownloader));
    }

    @Test
    public void connectionCheckTaskIsCancelled() throws Exception {
        File targetFile = mTemporaryFolder.newFile();
        when(mDownloader.getTargetFile()).thenReturn(targetFile);
        when(mDownloader.getDownloadedReleaseFilePath()).thenReturn(null);

        /* Cancel during execution. */
        mCheckTask = spy(mCheckTask);
        when(mCheckTask.isCancelled()).thenReturn(true);

        /* Perform background task. */
        mCheckTask.doInBackground();

        /* Verify */
        verifyZeroInteractions(ignoreStubs(mDownloader));
    }
}
