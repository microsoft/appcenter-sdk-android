/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.net.TrafficStats;
import android.net.Uri;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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

import static com.microsoft.appcenter.distribute.download.http.HttpConnectionDownloadFileTask.APK_CONTENT_TYPE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
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

    @Mock
    private Uri mMockDownloadUri;

    @Mock
    private File mMockTargetFile;

    @Mock
    private HttpConnectionReleaseDownloader mMockHttpDownloader;

    /**
     * Log tag for this service.
     */
    private static final String LOG_TAG = AppCenter.LOG_TAG + "Distribute";

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(AsyncTaskUtils.class);
        mockStatic(AppCenterLog.class);
        mockStatic(TrafficStats.class);
        mockStatic(System.class);
    }

    @Test
    public void doInBackgroundWhenTotalBytesDownloadedMoreZero() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock file. */
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(false);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(1);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn(null);

        /* Mock stream. */
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(1).thenReturn(-1);

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic(times(2));
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verify(mockBufferedInputStream).close();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadComplete(eq(mMockTargetFile));
    }

    @Test
    public void doInBackgroundVerifyRedirection() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        URL movedUrl = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "http://mock2";

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http")
                .thenReturn("https").thenReturn("http");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(movedUrl.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_MOVED_TEMP);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn("");

        /* Mock stream. */
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(-1);

        /* Mock file. */
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(true);

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic();
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, times(6)).disconnect();
        verify(mockBufferedInputStream).close();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader, never()).onDownloadComplete(eq(mMockTargetFile));
    }

    @Test
    public void doInBackgroundWhenTaskCancelled() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_MOVED_PERM);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentLength()).thenReturn(0);
        when(urlConnection.getContentType()).thenReturn(APK_CONTENT_TYPE);

        /* Mock stream. */
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(0);

        /* Start. */
        startDoInBackgroundWithSpy();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
        verify(mockBufferedInputStream).read(any(byte[].class));
        verify(urlConnection, never()).disconnect();
        verify(mockBufferedInputStream).close();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader, never()).onDownloadComplete(eq(mMockTargetFile));
    }

    @Test
    public void doInBackgroundWhenNowLowLastReportedTime() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock system time. */
        when(System.currentTimeMillis()).thenReturn(300L);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_SEE_OTHER);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn("");
        when(urlConnection.getContentLength()).thenReturn(1);

        /* Mock stream. */
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(0).thenReturn(-1);

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verify(mockBufferedInputStream).close();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader, never()).onDownloadComplete(eq(mMockTargetFile));
    }

    @Test
    public void doInBackgroundWhenFlushThrowIOException() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock system time. */
        when(System.currentTimeMillis()).thenReturn(0L);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_SEE_OTHER);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn("");
        when(urlConnection.getContentLength()).thenReturn(0);

        /* Mock stream. */
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(0).thenReturn(-1);
        doThrow(new IOException()).when(mockFileOutputStream).flush();

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verify(mockBufferedInputStream).close();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader, never()).onDownloadComplete(eq(mMockTargetFile));
        verify(mMockHttpDownloader).onDownloadError(anyString());
    }

    @Test
    public void doInBackgroundWhenTotalBytesDownloadedMoreThanUpdateThreshold() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock file. */
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(false);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(1);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn(null);

        /* Mock stream. */
        int bytesThreshold = 512 * 1024;
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(bytesThreshold)
                .thenReturn(2 * (bytesThreshold))
                .thenReturn(-1);

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic(times(2));
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verify(mockBufferedInputStream).close();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadComplete(eq(mMockTargetFile));
    }

    @Test
    public void doInBackgroundWhenTotalTimeMoreThanUpdateTimeThreshold() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock file. */
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(false);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(1);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn(null);

        /* Mock stream. */
        mockStatic(System.class);
        when(System.currentTimeMillis()).thenReturn(600L);
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(1).thenReturn(-1);

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic(times(2));
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verify(mockBufferedInputStream).close();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadComplete(eq(mMockTargetFile));
    }

    @Test
    public void doInBackgroundInputStreamNull() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock file. */
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(false);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(1);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn(null);

        /* Mock stream. */
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        whenNew(BufferedInputStream.class)
                .withParameterTypes(InputStream.class)
                .withArguments(mockInputStream).thenThrow(new FileNotFoundException("test"));

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic(times(2));
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadError("test");
    }

    @Test
    public void doInBackgroundOutputStreamNull() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        URL movedUrl = mock(URL.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock file. */
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(false);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(1);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn(null);

        /* Mock stream. */
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        whenNew(FileOutputStream.class)
                .withParameterTypes(File.class)
                .withArguments(mMockTargetFile).thenThrow(new FileNotFoundException("test"));

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic(times(2));
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadError("test");
    }

    @Test
    public void doInBackgroundIgnoredIOException() throws Exception {

        /* Prepare data. */
        URL url = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);
        URL movedUrl = mock(URL.class);
        FileOutputStream mockFileOutputStream = mock(FileOutputStream.class);
        BufferedInputStream mockBufferedInputStream = mock(BufferedInputStream.class);
        String urlString = "https://mock";
        String movedUrlString = "https://mock2";

        /* Mock file. */
        when(mMockTargetFile.exists()).thenReturn(true);
        when(mMockTargetFile.delete()).thenReturn(false);

        /* Mock url. */
        whenNew(URL.class).withArguments(eq(urlString)).thenReturn(url);
        whenNew(URL.class).withArguments(eq(movedUrlString)).thenReturn(movedUrl);
        when(url.getProtocol()).thenReturn("https");
        when(movedUrl.getProtocol()).thenReturn("https");
        when(mMockDownloadUri.toString()).thenReturn(urlString);

        /* Mock https connection. */
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(1);
        when(urlConnection.getHeaderField(anyString())).thenReturn(movedUrlString);
        when(urlConnection.getContentType()).thenReturn(null);

        /* Mock stream. */
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mockFileOutputStream);
        whenNew(BufferedInputStream.class).withAnyArguments().thenReturn(mockBufferedInputStream);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockBufferedInputStream.read(any(byte[].class))).thenReturn(1).thenReturn(-1);
        doThrow(new IOException()).when(mockFileOutputStream).close();

        /* Start. */
        startDoInBackground();
        HttpConnectionDownloadFileTask task = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(mMockHttpDownloader, mMockDownloadUri, mMockTargetFile));
        task.doInBackground(null);

        /* Verify. */
        verifyStatic(times(2));
        AppCenterLog.warn(anyString(), anyString());
        verify(urlConnection, never()).disconnect();
        verify(mockFileOutputStream).close();
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
        verify(mMockHttpDownloader).onDownloadComplete(mMockTargetFile);
    }


    private void startDoInBackground() {
        final HttpConnectionDownloadFileTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(HttpConnectionDownloadFileTask.class))).then(new Answer<HttpConnectionDownloadFileTask>() {
            @Override
            public HttpConnectionDownloadFileTask answer(InvocationOnMock invocation) {
                task[0] = (HttpConnectionDownloadFileTask) invocation.getArguments()[1];
                return task[0];
            }
        });
    }

    private void startDoInBackgroundWithSpy() {
        final HttpConnectionDownloadFileTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(HttpConnectionDownloadFileTask.class))).then(new Answer<HttpConnectionDownloadFileTask>() {
            @Override
            public HttpConnectionDownloadFileTask answer(InvocationOnMock invocation) {
                task[0] = spy((HttpConnectionDownloadFileTask) invocation.getArguments()[1]);
                doReturn(true).when(task[0]).isCancelled();
                return task[0];
            }
        });
    }
}
