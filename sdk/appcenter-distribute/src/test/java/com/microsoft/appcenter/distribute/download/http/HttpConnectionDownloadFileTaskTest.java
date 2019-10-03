/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.net.TrafficStats;
import android.net.Uri;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
        AsyncTaskUtils.class,
        AppCenterLog.class,
        HttpConnectionDownloadFileTask.class,
        TrafficStats.class,
        System.class
})
public class HttpConnectionDownloadFileTaskTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    private Uri mMockDownloadUri;

    @Mock
    private File mMockTargetFile;

    @Mock
    private HttpsURLConnection mUrlConnection;

    @Mock
    private BufferedInputStream mBufferedInputStream;

    @Mock
    private FileOutputStream mFileOutputStream;

    @Mock
    private InputStream mMockInputStream;

    @Mock
    private HttpConnectionReleaseDownloader mMockHttpDownloader;

    @Mock
    private URL mMovedUrlHttps;

    @Mock
    private URL mMovedUrlHttp;

    private HttpConnectionDownloadFileTask mTask;

    private static final String MOVED_URL_HTTPS = "https://mock2";

    private static final String MOVED_URL_HTTP = "http://mock2";

    private static final String APK_CONTENT_TYPE = "application/vnd.android.package-archive";

    @Before
    public void setUp() throws Exception {
        mockStatic(AsyncTaskUtils.class);
        mockStatic(AppCenterLog.class);
        mockStatic(TrafficStats.class);
        mockStatic(System.class);

        /* Prepare data. */
        URL url = mock(URL.class);
        String urlString = "https://mock";
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(false);

        /* Mock url. */
        when(url.getProtocol()).thenReturn("https");
        when(mMovedUrlHttps.getProtocol()).thenReturn("https");
        when(mMovedUrlHttp.getProtocol()).thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        when(url.openConnection()).thenReturn(mUrlConnection);
        when(mMovedUrlHttp.openConnection()).thenReturn(mUrlConnection);
        when(mMovedUrlHttps.openConnection()).thenReturn(mUrlConnection);
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(MOVED_URL_HTTPS)).thenReturn(mMovedUrlHttps);
        whenNew(URL.class).withArguments(eq(MOVED_URL_HTTP)).thenReturn(mMovedUrlHttp);

        when(mUrlConnection.getResponseCode()).thenReturn(1);
        when(mUrlConnection.getHeaderField(anyString())).thenReturn(MOVED_URL_HTTPS);
        when(mUrlConnection.getContentType()).thenReturn(null);

        /* Mock stream. */
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mBufferedInputStream);
        when(mUrlConnection.getInputStream()).thenReturn(mMockInputStream);
        when(mBufferedInputStream.read(any(byte[].class))).thenReturn(1).thenReturn(-1);

        mTask = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
    }

    private void verifyProperCompletion() throws Exception {
        verify(mBufferedInputStream).close();
        verify(mFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
    }

    private void verifyPrintWarn() {
        verifyStatic();
        AppCenterLog.warn(anyString(), anyString());
    }

    private void verifyNotPrintWarn() {
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
    }

    private void verifyDownloadComplete() {
        verify(mMockHttpDownloader).onDownloadComplete(eq(mMockTargetFile));
    }

    private void verifyDownloadNeverComplete() {
        verify(mMockHttpDownloader, never()).onDownloadComplete(eq(mMockTargetFile));
    }

    @Test
    public void doInBackgroundWhenTotalBytesDownloadedMoreZero() throws Exception {

        /* Start. */
        mTask.doInBackground();

        /* Verify. */
        verify(mUrlConnection, never()).disconnect();
        verifyPrintWarn();
        verifyProperCompletion();
        verifyDownloadComplete();
    }

    @Test
    public void doInBackgroundVerifyRedirection() throws Exception {
        when(mUrlConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_MOVED_TEMP);
        when(mUrlConnection.getHeaderField(anyString())).thenReturn(MOVED_URL_HTTP);
        when(mUrlConnection.getContentType()).thenReturn("");

        /* Mock stream. */
        when(mBufferedInputStream.read(any(byte[].class))).thenReturn(-1);

        /* Mock file. */
        when(mMockTargetFile.delete()).thenReturn(true);

        /* Start. */
        mTask.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, times(6)).disconnect();
        verifyProperCompletion();
        verifyDownloadNeverComplete();
    }

    @Test
    public void doInBackgroundWhenTaskCancelled() throws Exception {

        /* Mock https connection. */
        when(mUrlConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_MOVED_PERM);
        when(mUrlConnection.getContentLength()).thenReturn(0);
        when(mUrlConnection.getContentType()).thenReturn(APK_CONTENT_TYPE);

        /* Mock stream. */
        when(mBufferedInputStream.read(any(byte[].class))).thenReturn(0);

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task = spy(task);
        doReturn(true).when(task).isCancelled();
        task.doInBackground();

        /* Verify. */
        verifyNotPrintWarn();
        verify(mBufferedInputStream).read(any(byte[].class));
        verify(mUrlConnection, never()).disconnect();
        verifyProperCompletion();
        verifyDownloadNeverComplete();
    }

    @Test
    public void doInBackgroundWhenNowLowLastReportedTime() throws Exception {

        /* Mock system time. */
        when(System.currentTimeMillis()).thenReturn(300L);

        /* Mock url connection. */
        when(mUrlConnection.getContentType()).thenReturn("");
        when(mUrlConnection.getContentLength()).thenReturn(1);

        /* Mock stream. */
        when(mBufferedInputStream.read(any(byte[].class))).thenReturn(0).thenReturn(-1);

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, never()).disconnect();
        verifyProperCompletion();
        verifyDownloadNeverComplete();
    }

    @Test
    public void doInBackgroundWhenFlushThrowIOException() throws Exception {

        /* Mock system time. */
        when(System.currentTimeMillis()).thenReturn(0L);

        /* Mock https connection. */
        when(mUrlConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_SEE_OTHER);
        when(mUrlConnection.getContentType()).thenReturn("");
        when(mUrlConnection.getContentLength()).thenReturn(0);

        /* Mock stream. */
        when(mBufferedInputStream.read(any(byte[].class))).thenReturn(0).thenReturn(-1);
        doThrow(new IOException()).when(mFileOutputStream).flush();

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, never()).disconnect();
        verifyProperCompletion();
        verifyDownloadNeverComplete();
        verify(mMockHttpDownloader).onDownloadError(anyString());
    }

    @Test
    public void doInBackgroundWhenTotalBytesDownloadedMoreThanUpdateThreshold() throws Exception {

        /* Mock stream. */
        int bytesThreshold = 512 * 1024;
        when(mBufferedInputStream.read(any(byte[].class))).thenReturn(bytesThreshold)
                .thenReturn(2 * (bytesThreshold))
                .thenReturn(-1);

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, never()).disconnect();
        verifyProperCompletion();
        verifyDownloadComplete();
    }

    @Test
    public void doInBackgroundWhenTotalTimeMoreThanUpdateTimeThreshold() throws Exception {

        /* Mock stream. */
        when(System.currentTimeMillis()).thenReturn(600L);

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, never()).disconnect();
        verifyProperCompletion();
        verifyDownloadComplete();
    }

    @Test
    public void doInBackgroundInputStreamNull() throws Exception {

        /* Mock stream. */
        whenNew(BufferedInputStream.class)
                .withParameterTypes(InputStream.class)
                .withArguments(mMockInputStream).thenThrow(new FileNotFoundException("test"));

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, never()).disconnect();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadError("test");
    }

    @Test
    public void doInBackgroundOutputStreamNull() throws Exception {
        whenNew(FileOutputStream.class)
                .withParameterTypes(File.class)
                .withArguments(mMockTargetFile).thenThrow(new FileNotFoundException("test"));

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, never()).disconnect();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadError("test");
    }

    @Test
    public void doInBackgroundIgnoredIOException() throws Exception {
        doThrow(new IOException()).when(mFileOutputStream).close();

        /* Start. */
        HttpConnectionDownloadFileTask task = new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile);
        task.doInBackground();

        /* Verify. */
        verifyPrintWarn();
        verify(mUrlConnection, never()).disconnect();
        verify(mFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verifyDownloadComplete();
    }
}
