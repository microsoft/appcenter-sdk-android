package com.microsoft.appcenter.distribute.download.http;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Build;

import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

@PrepareForTest({AsyncTaskUtils.class, Uri.class})
//@PowerMockIgnore({"javax.net.ssl.*"})
@RunWith(PowerMockRunner.class)
public class HttpDownloadFileTaskTest {

    private Uri mockDownloadUri;
    private File mockTargetFile;
    private HttpConnectionReleaseDownloader mockHttpDownloader;

    @Before
    public void setUp() {
        mockTargetFile = mock(File.class);
        mockDownloadUri = mock(Uri.class);
        mockHttpDownloader = mock(HttpConnectionReleaseDownloader.class);
        mockStatic(AsyncTaskUtils.class);
    }

    @Test
    public void doInBackgroundWhenDirectoryNull() {
        when(mockTargetFile.getParentFile()).thenReturn(null);
        startDoInBackground();
        HttpDownloadFileTask task = AsyncTaskUtils.execute("TEST", new HttpDownloadFileTask(mockHttpDownloader, mockDownloadUri, mockTargetFile));
        task.doInBackground(null);
        verify(mockHttpDownloader).onDownloadError(anyString());
    }

    @Test
    public void doInBackgroundWhenDirectoryNotExists() {
        File mockFile = mock(File.class);
        when(mockTargetFile.getParentFile()).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(false);
        startDoInBackground();
        HttpDownloadFileTask task = AsyncTaskUtils.execute("TEST", new HttpDownloadFileTask(mockHttpDownloader, mockDownloadUri, mockTargetFile));
        task.doInBackground(null);
        verify(mockHttpDownloader).onDownloadError(anyString());
    }

    @Test
    public void doInBackgroundWhenDirectoryNotMkdirs() {
        File mockFile = mock(File.class);
        when(mockTargetFile.exists()).thenReturn(true);
        when(mockTargetFile.getParentFile()).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.mkdirs()).thenReturn(true);
        startDoInBackground();
        HttpDownloadFileTask task = AsyncTaskUtils.execute("TEST", new HttpDownloadFileTask(mockHttpDownloader, mockDownloadUri, mockTargetFile));
        task.doInBackground(null);
        verify(mockTargetFile).delete();
    }

    @Test
    public void doInBackgroundWhenTargetFileNotExists() {
        File mockFile = mock(File.class);
        when(mockTargetFile.getParentFile()).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.mkdirs()).thenReturn(true);
        startDoInBackground();
        HttpDownloadFileTask task = AsyncTaskUtils.execute("TEST", new HttpDownloadFileTask(mockHttpDownloader, mockDownloadUri, mockTargetFile));
        task.doInBackground(null);
        verify(mockHttpDownloader).onDownloadError(anyString());
    }

    @Test
    public void doInBackgroundWhenContentTypeInvalid() throws Exception {
        File mockFile = mock(File.class);
        URL mockUrl = mock(URL.class);
        URLConnection mockURLConnection = mock(URLConnection.class);
        when(mockDownloadUri.toString()).thenReturn("https://test");
        whenNew(URL.class).withArguments(any(Uri.class)).thenReturn(mockUrl);
        when(mockUrl.openConnection()).thenReturn(mockURLConnection);
        when(mockURLConnection.getContentType()).thenReturn("text");
//        when((mockURLConnection.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_ACCEPTED);
        when(mockTargetFile.exists()).thenReturn(false);
        when(mockTargetFile.getParentFile()).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.mkdirs()).thenReturn(true);
        startDoInBackground();
        HttpDownloadFileTask task = AsyncTaskUtils.execute("TEST", new HttpDownloadFileTask(mockHttpDownloader, mockDownloadUri, mockTargetFile));
        task.doInBackground(null);
        verify(mockHttpDownloader).onDownloadError(anyString());
    }

    private void startDoInBackground() {
        final HttpDownloadFileTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(HttpDownloadFileTask.class))).then(new Answer<HttpDownloadFileTask>() {
            @Override
            public HttpDownloadFileTask answer(InvocationOnMock invocation) {
                task[0] = spy((HttpDownloadFileTask) invocation.getArguments()[1]);
                return task[0];
            }
        });
    }
}
