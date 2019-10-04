/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.net.TrafficStats;
import android.net.Uri;

import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_PROGRESS_BYTES_THRESHOLD;
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_PROGRESS_TIME_THRESHOLD;
import static com.microsoft.appcenter.distribute.download.http.HttpConnectionDownloadFileTask.APK_CONTENT_TYPE;
import static com.microsoft.appcenter.http.HttpUtils.THREAD_STATS_TAG;
import static com.microsoft.appcenter.http.HttpUtils.WRITE_BUFFER_SIZE;
import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({
        AppCenterLog.class,
        HttpUtils.class,
        TrafficStats.class
})
@RunWith(PowerMockRunner.class)
public class HttpConnectionDownloadFileTaskTest {

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Mock
    private Uri mDownloadUri;

    @Mock
    private HttpsURLConnection mUrlConnection;

    @Mock
    private HttpConnectionReleaseDownloader mDownloader;

    private File mTargetFile;

    private HttpConnectionDownloadFileTask mDownloadFileTask;

    @Before
    public void setUp() throws Exception {
        mockStatic(TrafficStats.class);

        /* Mock Uri. */
        when(mDownloadUri.toString()).thenReturn("https://test/url");

        /* Mock URL connection. */
        mockStatic(HttpUtils.class);
        when(HttpUtils.createHttpsConnection(any(URL.class))).thenReturn(mUrlConnection);
        when(mUrlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mUrlConnection.getContentType()).thenReturn(APK_CONTENT_TYPE);

        /* Create target file. */
        mTargetFile = mTemporaryFolder.newFile();

        /* Create task. */
        mDownloadFileTask = new HttpConnectionDownloadFileTask(mDownloader, mDownloadUri, mTargetFile);
    }

    @After
    public void tearDown() {
        verifyStatic();
        TrafficStats.setThreadStatsTag(eq(THREAD_STATS_TAG));
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
    }

    @Test
    public void downloadSuccessful() throws IOException {
        String apk = "I'm an APK file";
        mockConnectionContent(apk);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verifyDownloadedContent(apk);
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader).onDownloadComplete(eq(mTargetFile));
        verify(mDownloader, never()).onDownloadError(anyString());
    }

    @Test
    public void wrongContentType() throws IOException {
        mockStatic(AppCenterLog.class);
        String apk = "I'm an APK file";
        mockConnectionContent(apk);
        when(mUrlConnection.getContentType()).thenReturn("text/html");

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verifyStatic();
        AppCenterLog.warn(eq(LOG_TAG), anyString());
        verifyDownloadedContent(apk);
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader).onDownloadComplete(eq(mTargetFile));
        verify(mDownloader, never()).onDownloadError(anyString());
    }

    @Test
    public void errorResponseCode() throws IOException {
        when(mUrlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onDownloadError(anyString());
    }

    @Test
    public void errorResponseCodeInvalid() throws IOException {
        when(mUrlConnection.getResponseCode()).thenReturn(-1);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onDownloadError(anyString());
    }

    @Test
    public void nothingIsDownloaded() throws IOException {
        when(mUrlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onDownloadError(anyString());
    }

    @Test
    public void readResponseError() throws IOException {
        when(mUrlConnection.getInputStream()).thenThrow(new IOException());

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onDownloadError(anyString());
    }

    @Test
    public void reportProgressBasedOnDownloadCount() throws IOException {
        long totalSize = UPDATE_PROGRESS_BYTES_THRESHOLD * 3;
        when(mUrlConnection.getContentLength()).thenReturn((int) totalSize);
        String apk = generateString((int) totalSize, '*');
        mockConnectionContent(apk);

        /* Mock system time. */
        mockStatic(System.class);
        when(System.currentTimeMillis()).thenAnswer(new Answer<Long>() {

            private long currentTime = 0;

            @Override
            public Long answer(InvocationOnMock invocation) {
                return currentTime += UPDATE_PROGRESS_TIME_THRESHOLD;
            }
        });

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verifyDownloadedContent(apk);
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader).onDownloadProgress(WRITE_BUFFER_SIZE, totalSize);
        verify(mDownloader).onDownloadProgress(WRITE_BUFFER_SIZE + UPDATE_PROGRESS_BYTES_THRESHOLD, totalSize);
        verify(mDownloader).onDownloadProgress(WRITE_BUFFER_SIZE + UPDATE_PROGRESS_BYTES_THRESHOLD * 2, totalSize);
        verify(mDownloader).onDownloadProgress(totalSize, totalSize);
        verify(mDownloader).onDownloadComplete(eq(mTargetFile));
        verify(mDownloader, never()).onDownloadError(anyString());
    }

    @Test
    public void reportProgressBasedOnDownloadTime() throws IOException {
        long totalSize = UPDATE_PROGRESS_BYTES_THRESHOLD * 3;
        when(mUrlConnection.getContentLength()).thenReturn((int) totalSize);
        String apk = generateString((int) totalSize, '*');
        mockConnectionContent(apk);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verifyDownloadedContent(apk);
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader).onDownloadProgress(WRITE_BUFFER_SIZE, totalSize);
        verify(mDownloader).onDownloadProgress(totalSize, totalSize);
        verify(mDownloader).onDownloadComplete(eq(mTargetFile));
        verify(mDownloader, never()).onDownloadError(anyString());
    }

    @Test
    public void cancelDuringDownloading() throws IOException {
        long totalSize = UPDATE_PROGRESS_BYTES_THRESHOLD * 3;
        when(mUrlConnection.getContentLength()).thenReturn((int) totalSize);
        String apk = generateString((int) totalSize, '*');
        mockConnectionContent(apk);

        mDownloadFileTask = spy(mDownloadFileTask);
        when(mDownloadFileTask.isCancelled()).thenReturn(true);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        assertEquals(WRITE_BUFFER_SIZE, mTargetFile.length());
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader).onDownloadProgress(WRITE_BUFFER_SIZE, totalSize);
        verify(mDownloader).onDownloadComplete(eq(mTargetFile));
        verify(mDownloader, never()).onDownloadError(anyString());
    }

    @Test
    public void errorOnStreamClose() throws IOException {
        String apk = "I'm an APK file";
        InputStream inputStream = spy(new ByteArrayInputStream(apk.getBytes()));
        doThrow(new IOException()).when(inputStream).close();
        when(mUrlConnection.getInputStream()).thenReturn(inputStream);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verifyDownloadedContent(apk);
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader).onDownloadComplete(eq(mTargetFile));
        verify(mDownloader, never()).onDownloadError(anyString());
    }

    @Test
    public void errorDuringStreamCopy() throws IOException {
        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException());
        when(mUrlConnection.getInputStream()).thenReturn(inputStream);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onDownloadError(anyString());
    }

    @Test
    public void errorDuringStreamCopyAndClose() throws IOException {
        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException());
        doThrow(new IOException()).when(inputStream).close();
        when(mUrlConnection.getInputStream()).thenReturn(inputStream);

        /* Perform background task. */
        mDownloadFileTask.doInBackground();

        /* Verify. */
        verify(mDownloader).onDownloadStarted(anyLong());
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onDownloadError(anyString());
    }

    private void mockConnectionContent(String content) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        when(mUrlConnection.getInputStream()).thenReturn(inputStream);
    }

    private void verifyDownloadedContent(String expected) throws IOException {
        String downloadedContent = new String(Files.readAllBytes(mTargetFile.toPath()), Charset.defaultCharset());
        assertEquals(expected, downloadedContent);
    }
}
