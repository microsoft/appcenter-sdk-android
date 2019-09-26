package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static android.content.Context.DOWNLOAD_SERVICE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest ({SharedPreferencesManager.class, AsyncTaskUtils.class, DownloadManager.Request.class, Uri.class, Build.VERSION.class, HandlerUtils.class,})
@RunWith(PowerMockRunner.class)
public class DownloadManagerReleaseDownloaderTest {

    private Context mockContext;
    private ReleaseDownloader.Listener mockListener;
    private DownloadManager mockDownloadManager;
    private Handler mAppCenterHandler;

    @Before
    public void setUp() throws Exception {
        mockContext = mock(Context.class);
        mockListener = mock(ReleaseDownloader.Listener.class);
        mockDownloadManager = mock(DownloadManager.class);
        when(mockContext.getSystemService(DOWNLOAD_SERVICE)).thenReturn(mockDownloadManager);
        mockStatic(SharedPreferencesManager.class);
        mockStatic(AsyncTaskUtils.class);
        mockStatic(Uri.class);
        mockStatic(HandlerUtils.class);
        whenNew(DownloadManager.class).withAnyArguments().thenReturn(mockDownloadManager);
    }

    @Test
    public void callDeleteWhenValidDownloadIdTest() throws Exception {

        /* Prepare data. */
        long validDownloadId = 1;
        long invalidDownloadId = -1;
        final DownloadManagerUpdateTask mockDownloadManagerUpdateTask = mock(DownloadManagerUpdateTask.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                return mockDownloadManagerUpdateTask;
            }
        });
        final DownloadManagerRequestTask mockDownloadManagerRequestTask = mock(DownloadManagerRequestTask.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).then(new Answer<DownloadManagerRequestTask>() {
            @Override
            public DownloadManagerRequestTask answer(InvocationOnMock invocation) {
                return mockDownloadManagerRequestTask;
            }
        });
        final DownloadManagerRemoveTask mockDownloadManagerRemoveTask = mock(DownloadManagerRemoveTask.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRemoveTask.class))).then(new Answer<DownloadManagerRemoveTask>() {
            @Override
            public DownloadManagerRemoveTask answer(InvocationOnMock invocation) {
                return mockDownloadManagerRemoveTask;
            }
        });
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);

        /* Verify when download id is invalid. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(invalidDownloadId);
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());

        /* Verify when download id is valid. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(validDownloadId);
        releaseDownloader.resume();
        verifyStatic(times(2));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerRequestTask.class), Mockito.<Void>anyVararg());

        /* Call delete. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID))).thenReturn(validDownloadId);
        releaseDownloader.delete();

        /* Verify that it is call. */
        verify(mockDownloadManagerRequestTask).cancel(eq(true));
        verify(mockDownloadManagerUpdateTask).cancel(eq(true));
        verifyStatic(times(3));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerRemoveTask.class), Mockito.<Void>anyVararg());
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void deleteWhenInvalidDownloadIdTest() throws Exception {

        /* Prepare data. */
        long invalidDownloadId = -1;
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(invalidDownloadId);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.delete();

        /* Verify. */
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerRemoveTask.class), Mockito.<Void>anyVararg());
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_ID));
    }

    @Test
    public void callOnRequestWhenMandatoryUpdateReturnTrueTest() throws Exception {

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
        ArgumentCaptor<DownloadManager.Request> captorManagerRequest = ArgumentCaptor.forClass(DownloadManager.Request.class);
        when(mockDownloadManager.enqueue(captorManagerRequest.capture())).thenReturn(mockNewDownloadId);

        /* Mock download task. */
        final DownloadManagerRequestTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).then(new Answer<DownloadManagerRequestTask>() {
            @Override
            public DownloadManagerRequestTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerRequestTask) invocation.getArguments()[1]);
                return task[0];
            }
        });

        /* Verify. */
        releaseDownloader.resume();
        task[0].doInBackground(null);
        DownloadManager.Request downloadRequest = captorManagerRequest.getValue();
        verify(mockListener).onStart(anyLong());
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(mockNewDownloadId));
        verify(mockDownloadManager).enqueue(eq(downloadRequest));
        verifyStatic(times(2));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void callOnRequestWhenMandatoryUpdateReturnFalseTest() throws Exception {

        /* Mock release details. */
        long mockPreviousDownloadId = -1L;
        long mockNewDownloadId = 2L;
        Uri mockUri = mock(Uri.class);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getDownloadUrl()).thenReturn(mockUri);
        when(mockReleaseDetails.isMandatoryUpdate()).thenReturn(false);

        /* Prepare data. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(mockPreviousDownloadId);
        DownloadManagerReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);

        /* Mock download manager request. */
        ArgumentCaptor<DownloadManager.Request> captorManagerRequest = ArgumentCaptor.forClass(DownloadManager.Request.class);
        when(mockDownloadManager.enqueue(captorManagerRequest.capture())).thenReturn(mockNewDownloadId);

        final DownloadManagerRequestTask[] task = {null};
        /* Mock download task. */
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).then(new Answer<DownloadManagerRequestTask>() {
            @Override
            public DownloadManagerRequestTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerRequestTask) invocation.getArguments()[1]);
                return task[0];
            }
        });

        /* Verify. */
        releaseDownloader.resume();
        task[0].doInBackground(null);
        DownloadManager.Request downloadRequest = captorManagerRequest.getValue();
        verify(mockListener).onStart(anyLong());
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(mockNewDownloadId));
        verify(mockDownloadManager).enqueue(eq(downloadRequest));
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void callOnRequestChangeWhenThreadNotEqualsTest() throws Exception {

        /* Mock release details. */
        long mockPreviousDownloadId = -1L;
        long mockNewDownloadId = 2L;
        Uri mockUri = mock(Uri.class);
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockReleaseDetails.getDownloadUrl()).thenReturn(mockUri);
        when(mockReleaseDetails.isMandatoryUpdate()).thenReturn(false);

        /* Prepare data. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(mockPreviousDownloadId);
        final DownloadManagerReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);

        /* Mock download manager request. */
        ArgumentCaptor<DownloadManager.Request> captorManagerRequest = ArgumentCaptor.forClass(DownloadManager.Request.class);
        when(mockDownloadManager.enqueue(captorManagerRequest.capture())).thenReturn(mockNewDownloadId);

        /* Mock download task. */
        final DownloadManagerRequestTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerRequestTask.class))).then(new Answer<DownloadManagerRequestTask>() {
            @Override
            public DownloadManagerRequestTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerRequestTask) invocation.getArguments()[1]);
                return new DownloadManagerRequestTask(releaseDownloader);
            }
        });

        /* Verify. */
        releaseDownloader.resume();
        task[0].doInBackground(null);
        verify(mockListener, never()).onStart(anyLong());
        verifyStatic(never());
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(mockNewDownloadId));
        DownloadManager.Request downloadRequest = captorManagerRequest.getValue();
        verify(mockDownloadManager).enqueue(eq(downloadRequest));
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mockDownloadManager).remove(eq(mockNewDownloadId));
    }

    @Test
    public void callOnUpdateWhenCursorNull() {

        /* Prepare data. */
        long validDownloadId = 1;
        final DownloadManagerUpdateTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerUpdateTask) invocation.getArguments()[1]);
                return task[0];
            }
        });
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        when(mockDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(null);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(validDownloadId);

        /* Verify. */
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        task[0].doInBackground(null);
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mockListener).onError(anyString());
    }

    @Test
    public void callOnUpdateWhenCursorMoveToFirstFalse() {

        /* Prepare data. */
        long validDownloadId = 1;
        final DownloadManagerUpdateTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerUpdateTask) invocation.getArguments()[1]);
                return task[0];
            }
        });
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        Cursor mockCursor = mock(Cursor.class);
        when(mockDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(false);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(validDownloadId);

        /* Verify. */
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        task[0].doInBackground(null);
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mockListener).onError(anyString());
    }

    @Test
    public void callOnUpdateWhenCursorReturnStatusFaild() {

        /* Prepare data. */
        long validDownloadId = 1;
        final DownloadManagerUpdateTask[] task = {null};
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        Cursor mockCursor = mock(Cursor.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerUpdateTask) invocation.getArguments()[1]);
                return task[0];
            }
        });
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        when(mockDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getInt(anyInt())).thenReturn(DownloadManager.STATUS_FAILED);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(validDownloadId);

        /* Verify. */
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        task[0].doInBackground(null);
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mockListener).onError(anyString());
    }

    @Test
    public void callOnUpdateWhenCursorReturnStatusNotSuccessfulAndProgressReturnTrue() throws Exception {

        /* Prepare data. */
        long validDownloadId = 1;
        long mockSize = 1;
        final DownloadManagerUpdateTask[] task = {null};
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        Cursor mockCursor = mock(Cursor.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerUpdateTask) invocation.getArguments()[1]);
                return task[0];
            }
        });
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        when(mockDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getInt(anyInt())).thenReturn(DownloadManager.STATUS_PAUSED);
        when(mockCursor.getLong(anyInt())).thenReturn(mockSize);
        when(mockListener.onProgress(anyInt(), anyInt())).thenReturn(true);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(validDownloadId);

        /* Verify. */
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        task[0].doInBackground(null);
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        //todo call handler
    }


    @Test
    public void callOnUpdateWhenCursorReturnStatusSuccessfulAndOnCompleteReturnTrue() {

        /* Prepare data. */
        long validDownloadId = 1;
        Uri mockUri = mock(Uri.class);
        final DownloadManagerUpdateTask[] task = {null};
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        Cursor mockCursor = mock(Cursor.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerUpdateTask) invocation.getArguments()[1]);
                return task[0];
            }
        });
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        when(mockDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getInt(anyInt())).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(validDownloadId);
        when(Uri.parse(anyString())).thenReturn(mockUri);
        when(mockListener.onComplete(any(Uri.class))).thenReturn(true);

        /* Verify. */
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        task[0].doInBackground(null);
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mockCursor).close();
    }

    @Test
    public void callOnUpdateWhenCursorReturnStatusSuccessfulAndOnCompleteReturnFalse() {

        /* Prepare data. */
        long validDownloadId = 1;
        Uri mockUri = mock(Uri.class);
        final DownloadManagerUpdateTask[] task = {null};
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);
        Cursor mockCursor = mock(Cursor.class);
        when(AsyncTaskUtils.execute(anyString(), isA(DownloadManagerUpdateTask.class))).then(new Answer<DownloadManagerUpdateTask>() {
            @Override
            public DownloadManagerUpdateTask answer(InvocationOnMock invocation) {
                task[0] = spy((DownloadManagerUpdateTask) invocation.getArguments()[1]);
                return task[0];
            }
        });
        when(mockDownloadManager.enqueue(any(DownloadManager.Request.class))).thenReturn(1L);
        when(mockDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getInt(anyInt())).thenReturn(DownloadManager.STATUS_SUCCESSFUL);
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_ID), eq(INVALID_DOWNLOAD_IDENTIFIER))).thenReturn(validDownloadId);
        when(Uri.parse(anyString())).thenReturn(mockUri);
        when(mockListener.onComplete(any(Uri.class))).thenReturn(false);

        /* Mock sdk build version. */
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", 16);

        /* Verify. */
        ReleaseDownloader releaseDownloader = new DownloadManagerReleaseDownloader(mockContext, mockReleaseDetails, mockListener);
        releaseDownloader.resume();
        task[0].doInBackground(null);
        verifyStatic(times(1));
        AsyncTaskUtils.execute(anyString(), any(DownloadManagerUpdateTask.class), Mockito.<Void>anyVararg());
        verify(mockListener).onError(anyString());
        verify(mockCursor).close();
    }

    @After
    public void onAfterTest() {
        mockContext = null;
        mockDownloadManager = null;
        mockListener = null;
    }
}
