package com.microsoft.azure.mobile.persistence;

import android.os.Handler;

import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings({"unused", "CanBeFinal"})
@PrepareForTest(DatabasePersistenceAsync.class)
public class DatabasePersistenceAsyncTest {

    private static final String GROUP = "test";

    @Mock
    private final DatabasePersistenceAsync.DatabasePersistenceAsyncCallback mCallback = spy(new DatabasePersistenceAsync.AbstractDatabasePersistenceAsyncCallback() {

        @Override
        public void onSuccess(Object result) {
        }
    });

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private Handler mHandler;

    @Mock
    private Persistence mPersistence;

    private DatabasePersistenceAsync mDatabase;

    @Before
    public void setUp() throws Exception {
        whenNew(Handler.class).withAnyArguments().thenReturn(mHandler);
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mHandler).post(any(Runnable.class));
        mDatabase = new DatabasePersistenceAsync(mPersistence);
    }

    @Test
    public void putLog() throws Persistence.PersistenceException {
        Log log = mock(Log.class);
        mDatabase.putLog(GROUP, log, mCallback);
        verify(mPersistence).putLog(GROUP, log);
        verify(mCallback).onSuccess(null);
    }

    @Test
    public void putLogFailure() throws Persistence.PersistenceException {
        doThrow(new Persistence.PersistenceException("", new IOException())).when(mPersistence).putLog(anyString(), any(Log.class));
        Log log = mock(Log.class);
        mDatabase.putLog(GROUP, log, mCallback);
        verify(mPersistence).putLog(GROUP, log);
        verify(mCallback).onFailure(notNull(Persistence.PersistenceException.class));
    }

    @Test
    public void deleteLogsById() {
        mDatabase.deleteLogs(GROUP, "id");
        verify(mPersistence).deleteLogs(GROUP, "id");
    }

    @Test
    public void deleteLogsByIdWithCallback() {
        mDatabase.deleteLogs(GROUP, "id", mCallback);
        verify(mPersistence).deleteLogs(GROUP, "id");
        verify(mCallback).onSuccess(null);
    }

    @Test
    public void deleteLogsByGroup() {
        mDatabase.deleteLogs(GROUP);
        verify(mPersistence).deleteLogs(GROUP);
    }

    @Test
    public void deleteLogsByGroupWithCallback() {
        mDatabase.deleteLogs(GROUP, mCallback);
        verify(mPersistence).deleteLogs(GROUP);
        verify(mCallback).onSuccess(null);
    }

    @Test
    public void countLogs() {
        int count = 3;
        when(mPersistence.countLogs(GROUP)).thenReturn(count);
        mDatabase.countLogs(GROUP, mCallback);
        verify(mPersistence).countLogs(GROUP);
        verify(mCallback).onSuccess(count);
    }

    @Test
    public void getLogs() {
        int limit = 35;
        List<Log> outLogs = new ArrayList<>();
        String batchId = UUID.randomUUID().toString();
        when(mPersistence.getLogs(GROUP, limit, outLogs)).thenReturn(batchId);
        mDatabase.getLogs(GROUP, limit, outLogs, mCallback);
        verify(mPersistence).getLogs(GROUP, limit, outLogs);
        verify(mCallback).onSuccess(batchId);
    }

    @Test
    public void clearPendingLogState() {
        mDatabase.clearPendingLogState();
        verify(mPersistence).clearPendingLogState();
    }

    @Test
    public void clearPendingLogStateWithCallback() {
        mDatabase.clearPendingLogState(mCallback);
        verify(mPersistence).clearPendingLogState();
        verify(mCallback).onSuccess(null);
    }

    @Test
    public void close() throws IOException {
        mDatabase.close();
        verify(mPersistence).close();
    }

    @Test
    public void closeWithCallback() throws IOException {
        mDatabase.close(mCallback);
        verify(mPersistence).close();
        verify(mCallback).onSuccess(null);
    }

    @Test
    public void closeWithCallbackFailed() throws IOException {
        doThrow(new IOException()).when(mPersistence).close();
        mDatabase.close(mCallback);
        verify(mPersistence).close();
        verify(mCallback).onFailure(notNull(IOException.class));
    }

    @Test
    @PrepareForTest(MobileCenterLog.class)
    public void waitForCurrentTasksToComplete() throws InterruptedException {
        mockStatic(MobileCenterLog.class);
        mDatabase.waitForCurrentTasksToComplete(5000);
        verifyStatic();
        MobileCenterLog.debug(anyString(), anyString());
    }

    @Test(expected = InterruptedException.class)
    public void waitForCurrentInterrupted() throws Exception {
        Semaphore semaphore = mock(Semaphore.class);
        whenNew(Semaphore.class).withAnyArguments().thenReturn(semaphore);
        when(semaphore.tryAcquire(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());
        mDatabase.waitForCurrentTasksToComplete(5000);
    }

    @Test
    @PrepareForTest(MobileCenterLog.class)
    public void waitForCurrentTimeout() throws Exception {
        mockStatic(MobileCenterLog.class);
        Semaphore semaphore = mock(Semaphore.class);
        whenNew(Semaphore.class).withAnyArguments().thenReturn(semaphore);
        when(semaphore.tryAcquire(anyLong(), any(TimeUnit.class))).thenReturn(false);
        mDatabase.waitForCurrentTasksToComplete(5000);
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString());
    }
}
