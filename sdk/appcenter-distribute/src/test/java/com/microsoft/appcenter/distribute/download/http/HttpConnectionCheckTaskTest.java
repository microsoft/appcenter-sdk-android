package com.microsoft.appcenter.distribute.download.http;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.nio.file.Files;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({
        HttpConnectionCheckTask.class,
        File.class,
        SharedPreferencesManager.class
})
public class HttpConnectionCheckTaskTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private HttpConnectionCheckTask mTask;

    @Mock
    private File mTargetFile;

    @Mock
    private HttpConnectionReleaseDownloader mDownloader;

    @Before
    public void setUp() {
        mTask = new HttpConnectionCheckTask(mDownloader);
    }

    @Test
    public void errorIfTargetFileIsNull() {
        when(mDownloader.getTargetFile()).thenReturn(null);
        mTask.doInBackground(null);

        /* Verify that onDownloadError callback is called. */
        verify(mDownloader).onDownloadError(anyString());
        verify(mDownloader, never()).onStart(any(File.class));
    }

    @Test
    public void downloadedFileExists() throws Exception {
        when(mDownloader.getTargetFile()).thenReturn(mTargetFile);
        mockStatic(SharedPreferencesManager.class);
        String downloadedReleaseFilePath = "test_path";

        /* Mock target file path equals to the downloaded file path. */
        File downloadFile = mTemporaryFolder.newFile(downloadedReleaseFilePath);
        when(mTargetFile.getAbsolutePath()).thenReturn(downloadFile.getPath());
        PowerMockito.when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null)).thenReturn(downloadFile.getPath());

        mTask.doInBackground(null);

        /* Verify that onDownloadComplete callback is called with the right target file. */
        verify(mDownloader).onDownloadComplete(mTargetFile);
        verify(mDownloader, never()).onStart(mTargetFile);
    }

    @Test
    public void downloadedFilePathEqualToTargetButNotExist() throws Exception {
        when(mDownloader.getTargetFile()).thenReturn(mTargetFile);
        mockStatic(SharedPreferencesManager.class);
        String downloadedReleaseFilePath = "test_path";

        /* Mock target file path equals to the downloaded file path. */
        when(mTargetFile.getAbsolutePath()).thenReturn(downloadedReleaseFilePath);
        PowerMockito.when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null)).thenReturn(downloadedReleaseFilePath);

        mTask.doInBackground(null);

        /* Verify that onDownloadComplete callback is not called. */
        verify(mDownloader, never()).onDownloadComplete(mTargetFile);
        verify(mDownloader).onStart(mTargetFile);
    }

    @Test
    public void downloadedFilePathNotEqualToTargetFilePath() throws Exception {
        when(mDownloader.getTargetFile()).thenReturn(mTargetFile);
        mockStatic(SharedPreferencesManager.class);
        String downloadedReleaseFilePath = "test_path";
        String targetFilePath = "test_path-2";
        File downloadFile = mTemporaryFolder.newFile(downloadedReleaseFilePath);

        /* Mock target file path equals to the downloaded file path. */
        when(mTargetFile.getAbsolutePath()).thenReturn(targetFilePath);
        PowerMockito.when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null)).thenReturn(downloadFile.getPath());

        mTask.doInBackground(null);

        /* Verify that the previous file is deleted. */
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE));
        assertFalse(Files.exists(downloadFile.toPath()));

        /* Verify that onDownloadComplete callback is not called. */
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onStart(mTargetFile);
    }

    @Test
    public void connectionCheckTaskIsCancelled() {
        when(mDownloader.getTargetFile()).thenReturn(mTargetFile);
        mockStatic(SharedPreferencesManager.class);

        /* Mock returning null path to file. */
        PowerMockito.when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null)).thenReturn(null);

        HttpConnectionCheckTask task = new HttpConnectionCheckTask(mDownloader);
        task = spy(task);
        doReturn(true).when(task).isCancelled();
        task.doInBackground(null);

        /* Verify that onStart/onDownloadComplete is never called if the task is cancelled. */
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader, never()).onStart(any(File.class));
    }

    @Test
    public void downloadedFilePathIsNull() {
        when(mDownloader.getTargetFile()).thenReturn(mTargetFile);
        mockStatic(SharedPreferencesManager.class);

        /* Mock returning null path to file. */
        PowerMockito.when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null)).thenReturn(null);

        mTask.doInBackground(null);

        /* Verify that onStart is called. */
        verify(mDownloader, never()).onDownloadComplete(any(File.class));
        verify(mDownloader).onStart(mTargetFile);
    }
}
