package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.internal.matchers.CapturesArguments;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest ( {SharedPreferencesManager.class, AsyncTaskUtils.class, DownloadManager.Request.class})
@RunWith(PowerMockRunner.class)
public class DownloadManagerReleaseDownloaderTest {

    Context mockContext;

    ReleaseDownloader.Listener mockListener;

    DownloadManager mockDownloadManager;

    Semaphore mCheckDownloadBeforeSemaphore;

    Semaphore mCheckDownloadAfterSemaphore;

    @Before
    public void setUp() throws Exception {
        mockContext = mock(Context.class);
        mockListener = mock(ReleaseDownloader.Listener.class);
        mockDownloadManager = mock(DownloadManager.class);
        when(mockContext.getSystemService(DOWNLOAD_SERVICE)).thenReturn(mockDownloadManager);
        mockStatic(SharedPreferencesManager.class);
        mockStatic(AsyncTaskUtils.class);
        whenNew(DownloadManager.class).withAnyArguments().thenReturn(mockDownloadManager);
    }

    @Test
    public void resumeTest() throws Exception {

        /* Prepare data. */
        DownloadManagerUpdateTask mockDownloadManagerUpdateTask = mock(DownloadManagerUpdateTask.class);
        DownloadManagerRequestTask mockDownloadManagerRequestTask = mock(DownloadManagerRequestTask.class);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID))).thenReturn(-1L);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        whenNew(DownloadManagerRequestTask.class).withArguments(eq(releaseDownloader)).thenReturn(mockDownloadManagerRequestTask);

        /* Verify when downloadId == -1 and DownloadManagerReleaseDownloader was call. */
        releaseDownloader.resume();
//        verify(whenNew(DownloadManagerRequestTask.class).withArguments(eq(releaseDownloader)));
        //todo async

        /* Verify when downloadId == 1 and DownloadManagerUpdateTask was call. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID))).thenReturn(1L);
        releaseDownloader.resume();
        verify(whenNew(DownloadManagerUpdateTask.class).withArguments(eq(releaseDownloader)));
    }

    @Test
    public void deleteWhenValidDownloadIdTest() throws Exception {

        /* Prepare data. */
        DownloadManagerUpdateTask mockDownloadManagerUpdateTask = mock(DownloadManagerUpdateTask.class);
        DownloadManagerRequestTask mockDownloadManagerRequestTask = mock(DownloadManagerRequestTask.class);
        DownloadManagerRemoveTask mockDownloadManagerRemoveTask = mock(DownloadManagerRemoveTask.class);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(-1L);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        whenNew(DownloadManagerUpdateTask.class).withAnyArguments().thenReturn(mockDownloadManagerUpdateTask);
        whenNew(DownloadManagerRequestTask.class).withAnyArguments().thenReturn(mockDownloadManagerRequestTask);
        whenNew(DownloadManagerRemoveTask.class).withAnyArguments().thenReturn(mockDownloadManagerRemoveTask);
        /* Verify when downloadId == -1 and DownloadManagerReleaseDownloader was call. */
        releaseDownloader.resume();
//        verify(whenNew(DownloadManagerRequestTask.class).withArguments(eq(releaseDownloader)));
            //todo verify async

        /* Verify when downloadId == 1 and DownloadManagerUpdateTask was call. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID))).thenReturn(1L);
        releaseDownloader.resume();
//        verify(whenNew(DownloadManagerUpdateTask.class).withArguments(eq(releaseDownloader)));
        // //todo verify async

        /* Prepare data and call. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID))).thenReturn(1L);
        releaseDownloader.cancel();

        /* Verify that it is call. */
        verify(mockDownloadManagerRequestTask).cancel(eq(true));
        verify(mockDownloadManagerUpdateTask).cancel(eq(true));
        // todo async
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));

        /* Prepare data and call. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID))).thenReturn(-1L);
        releaseDownloader.cancel();

        /* Verify that it is not call. */
        verify(mockDownloadManagerRequestTask.cancel(eq(true)));
        verify(mockDownloadManagerUpdateTask.cancel(eq(true)));

        // todo async
//        verify(whenNew(DownloadManagerRemoveTask.class).withArguments(eq(mockContext), eq(1)));
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void onRequestWhenMandatoryUpdateTest() throws Exception {

        /* Mock release details. */
        long mockPreviousDownloadId = -1L;
        long mockNewDownloadId = 2L;
        Uri mockUri = mock(Uri.class);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getDownloadUrl()).thenReturn(mockUri);
        when(mockReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        /* Prepare data. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(mockPreviousDownloadId);
        DownloadManagerReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);

        /* Mock download manager request. */
        DownloadManager.Request mockDownloadRequest = mock(DownloadManager.Request.class);
        whenNew(DownloadManager.Request.class).withAnyArguments().thenReturn(mockDownloadRequest);
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(mockNewDownloadId);

        /* Mock download task. */
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).then(new Answer<DownloadManagerRequestTask>() {
            @Override
            public DownloadManagerRequestTask answer(InvocationOnMock invocation) {
                DownloadManagerRequestTask task = spy((DownloadManagerRequestTask) invocation.getArguments()[1]);
                task.doInBackground(null);
                return task;
            }
        });

        /* Verify. */
        releaseDownloader.resume();
        verify(mockDownloadRequest).setNotificationVisibility(anyInt());
        verify(mockDownloadRequest).setVisibleInDownloadsUi(eq(false));
        verify(mockDownloadManager).enqueue(eq(mockDownloadRequest));
    }

    @Test
    public void onRequestWhenNotMandatoryUpdateTest() throws Exception {

        /* Mock release details. */
        long mockPreviousDownloadId = 123;
        Uri mockUri = mock(Uri.class);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getDownloadUrl()).thenReturn(mockUri);
        when(mockReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        /* Prepare data. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(mockPreviousDownloadId);
        final DownloadManagerReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);

        /* Mock download manager request. */
        final DownloadManager.Request mockDownloadRequest = mock(DownloadManager.Request.class);
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(mockPreviousDownloadId);
        whenNew(DownloadManager.Request.class).withArguments(any()).thenReturn(mockDownloadRequest);

        /* Mock download task. */
        final DownloadManagerUpdateTask mockDownloadManagerUpdateTask = mock(DownloadManagerUpdateTask.class);
        whenNew(DownloadManagerUpdateTask.class).withAnyArguments().thenReturn(mockDownloadManagerUpdateTask);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                new Thread() {

                    @Override
                    public void run() {
                        releaseDownloader.onUpdate();
                    }
                }.start();
                return mockDownloadManagerUpdateTask;
            }
        });

        /* Verify. */
        releaseDownloader.resume();
        verify(mockDownloadManager.enqueue(eq(mockDownloadRequest)));
    }

    @After
    public void onAfterTest() {
        mockContext = null;
        mockDownloadManager = null;
        mockListener = null;
    }
}
