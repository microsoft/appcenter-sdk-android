package com.microsoft.appcenter.channel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.CancellationException;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.AppCenterIngestion;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.persistence.Persistence;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

@SuppressWarnings("unused")
public class DefaultChannelTest extends AbstractDefaultChannelTest {

    @Test
    public void invalidGroup() throws Persistence.PersistenceException {
        Persistence persistence = mock(Persistence.class);
        Channel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, mock(Ingestion.class), mAppCenterHandler);

        /* Enqueue a log before group is registered = failure. */
        Log log = mock(Log.class);
        channel.enqueue(log, TEST_GROUP);
        verify(log, never()).setDevice(any(Device.class));
        verify(log, never()).setTimestamp(any(Date.class));
        verify(persistence, never()).putLog(TEST_GROUP, log);

        /* Trying remove group that not registered. */
        channel.removeGroup(TEST_GROUP);
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsSuccess() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(1)).then(getGetLogsAnswer(2));

        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);

        /* Enqueuing 49 events. */
        for (int i = 1; i <= 49; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
            assertEquals(i, channel.getCounter(TEST_GROUP));
        }
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));

        /* Enqueue another event. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));

        /* The counter should be 0 as we reset the counter after reaching the limit of 50. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify that 5 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion. */
        verify(mockIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the Persistence. */
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* Verify that we have called onBeforeSending in the listener. */
        verify(mockListener, times(50)).onBeforeSending(any(Log.class));

        /* Verify that we have called onSuccess in the listener. */
        verify(mockListener, times(50)).onSuccess(any(Log.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Prepare to mock timer. */
        AtomicReference<Runnable> runnable = catchPostRunnable();

        /* Schedule only 1 log after that. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        assertEquals(1, channel.getCounter(TEST_GROUP));
        verify(mockPersistence, times(51)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(50)).onSuccess(any(Log.class));

        /* Simulate the timer. */
        assertNotNull(runnable.get());
        runnable.get().run();
        runnable.set(null);

        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mockIngestion, times(2)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(51)).onSuccess(any(Log.class));

        /* 2 more timed logs. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        assertEquals(2, channel.getCounter(TEST_GROUP));
        verify(mockPersistence, times(53)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, times(2)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(51)).onSuccess(any(Log.class));

        /* Simulate the timer. */
        assertNotNull(runnable.get());
        runnable.get().run();

        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mockIngestion, times(3)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(3)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(53)).onSuccess(any(Log.class));

        /* Check total timers. */
        verify(mAppCenterHandler, times(3)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));

        /* Check channel clear. */
        channel.clear(TEST_GROUP);
        verify(mockPersistence).deleteLogs(eq(TEST_GROUP));
    }

    @Test
    public void lessLogsThanExpected() {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), Matchers.<ArrayList<Log>>any())).then(getGetLogsAnswer(40));

        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);

        /* Enqueuing 49 events. */
        for (int i = 1; i <= 49; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
            assertEquals(i, channel.getCounter(TEST_GROUP));
        }
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));

        /* Enqueue another event. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));

        /* Database returned less logs than we expected (40 vs 50), yet counter must be reset. */
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @NonNull
    private AtomicReference<Runnable> catchPostRunnable() {
        final AtomicReference<Runnable> runnable = new AtomicReference<>();
        when(mAppCenterHandler.postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                runnable.set((Runnable) invocation.getArguments()[0]);
                return true;
            }
        });
        return runnable;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void maxRequests() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);

        /* We make second request return less logs than expected to make sure counter is reset properly. */
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer()).then(getGetLogsAnswer(49)).then(getGetLogsAnswer());

        final List<ServiceCallback> callbacks = new ArrayList<>();
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    callbacks.add((ServiceCallback) invocation.getArguments()[3]);
                }
                return null;
            }
        });

        /* Init channel with mocks. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Enqueue enough logs to be split in N + 1 maximum requests. */
        for (int i = 0; i < 200; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mAppCenterHandler, times(4)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, times(4)).removeCallbacks(any(Runnable.class));

        /* Verify all logs stored, N requests sent, not log deleted yet. */
        verify(mockPersistence, times(200)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, times(3)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Make 1 of the call succeed. Verify log deleted. */
        callbacks.get(0).onCallSucceeded("");
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* The request N+1 is now unlocked. */
        verify(mockIngestion, times(4)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Unlock all requests and check logs deleted. */
        for (int i = 1; i < 4; i++)
            callbacks.get(i).onCallSucceeded("");
        verify(mockPersistence, times(4)).deleteLogs(any(String.class), any(String.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void maxRequestsInitial() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);

        when(mockPersistence.countLogs(any(String.class))).thenReturn(100);
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());

        final List<ServiceCallback> callbacks = new ArrayList<>();
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    callbacks.add((ServiceCallback) invocation.getArguments()[3]);
                }
                return null;
            }
        });

        /* Init channel with mocks. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Enqueue enough logs to be split in N + 1 maximum requests. */
        for (int i = 0; i < 100; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }

        /* Verify all logs stored, N requests sent, not log deleted yet. */
        verify(mockPersistence, times(100)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, times(3)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Make 1 of the call succeed. Verify log deleted. */
        callbacks.get(0).onCallSucceeded("");
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* The request N+1 is now unlocked. */
        verify(mockIngestion, times(4)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Unlock all requests and check logs deleted. */
        for (int i = 1; i < 4; i++)
            callbacks.get(i).onCallSucceeded("");
        verify(mockPersistence, times(4)).deleteLogs(any(String.class), any(String.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Only 2 batches after channel start (non initial logs), verify timer interactions. */
        verify(mAppCenterHandler, times(2)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, times(2)).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsRecoverable() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class)))
                .then(getGetLogsAnswer(50))
                .then(getGetLogsAnswer(50))
                .then(getGetLogsAnswer(20));
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new SocketException())).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);

        /* Enqueuing 50 events. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));

        /* Verify that 50 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion. */
        verify(mockIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have not called deleteLogs on the Persistence. */
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Verify that the Channel is disabled. */
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).deleteLogs(TEST_GROUP);

        /* Enqueuing 20 more events. */
        for (int i = 0; i < 20; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }

        /* The counter keeps being increased. */
        assertEquals(70, channel.getCounter(TEST_GROUP));

        /* Prepare to mock timer. */
        AtomicReference<Runnable> runnable = catchPostRunnable();

        /* Enable channel. */
        channel.setEnabled(true);

        /* Upon enabling, 1st batch of 50 is sent immediately, 20 logs are remaining. */
        assertEquals(20, channel.getCounter(TEST_GROUP));

        /* Wait for timer. */
        assertNotNull(runnable.get());
        runnable.get().run();

        /* The counter should be 0 after the second batch. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify that we have called sendAsync on the ingestion 3 times total. */
        verify(mockIngestion, times(3)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the Persistence (2 successful batches, the first call was a recoverable failure). */
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));

        /* Verify that we have called onBeforeSending in the listener. getLogs will return 50, 50 and 20. */
        verify(mockListener, times(120)).onBeforeSending(any(Log.class));

        /* Intermediate failures never forwarded to listener, only final success */
        verify(mockListener, never()).onFailure(any(Log.class), any(Exception.class));
        verify(mockListener, times(70)).onSuccess(any(Log.class));

        /* Verify timer. */
        verify(mAppCenterHandler, times(2)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsFatal() throws Exception {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class)))
                .then(getGetLogsAnswer(50))
                /* Second 50 logs will be used for clearing pending states. */
                .then(getGetLogsAnswer(50))
                .then(getGetLogsAnswer(20));
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new HttpException(403))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Enqueuing 50 events. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));

        /* Verify that 50 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion. */
        verify(mockIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that the Channel is disabled. */
        assertFalse(channel.isEnabled());

        /* Verify that we have cleared the logs. */
        verify(mockPersistence).deleteLogs(TEST_GROUP);

        /* Verify counter. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Enqueuing 20 more events. */
        for (int i = 0; i < 20; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }

        /* The counter should still be 0 as logs are discarded by channel now. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* No more timer yet at this point. */
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));

        /* Prepare to mock timer. */
        AtomicReference<Runnable> runnable = catchPostRunnable();

        /* Enable channel to see if it can work again after that error state. */
        channel.setEnabled(true);

        /* Enqueuing 20 more events. */
        for (int i = 0; i < 20; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        assertEquals(20, channel.getCounter(TEST_GROUP));

        /* Wait for timer. */
        assertNotNull(runnable.get());
        runnable.get().run();

        /* The counter should back to 0 now. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify that we have called sendAsync on the ingestion 2 times total: 1 earlier failure then 1 success. */
        verify(mockIngestion, times(2)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the Persistence for the successful batch after re-enabling. */
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* Verify 1 more timer call. */
        verify(mAppCenterHandler, times(2)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));

        /* Verify no more cancel timer. */
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void errorLogSuccess() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion mockIngestion = mock(Ingestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);

        /* Enqueuing 2 error logs. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify that 2 items have been persisted. */
        verify(mockPersistence, times(2)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion twice as batch size is 1. */
        verify(mockIngestion, times(2)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the Persistence. */
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));

        /* Verify that we have called onBeforeSending in the listener. */
        verify(mockListener, times(2)).onBeforeSending(any(Log.class));

        /* Verify that we have called onSuccess in the listener. */
        verify(mockListener, times(2)).onSuccess(any(Log.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify timer. */
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void errorLogRecoverable() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion mockIngestion = mock(Ingestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(1));
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new SocketException())).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);

        /* Enqueuing n errors. */
        int logNumber = 5;
        for (int i = 0; i < logNumber; i++)
            channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify that n items have been persisted. */
        verify(mockPersistence, times(logNumber)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion once for the first item, but not more than that. */
        verify(mockIngestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have not called deleteLogs on the Persistence. */
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Verify that we have called onBeforeSending in the listener. */
        verify(mockListener).onBeforeSending(any(Log.class));

        /* Verify that we have not called the failure listener. It's a transient exception that will be retried later when the channel is re-enabled. */
        verify(mockListener, never()).onFailure(any(Log.class), any(Exception.class));

        /* Verify that the Channel is disabled. */
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).deleteLogs(TEST_GROUP);

        /* Verify timer. */
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));

        channel.setEnabled(true);

        /* Verify that we have called sendAsync on the ingestion n+1 times total: 1 failure before re-enabling, n success after. */
        verify(mockIngestion, times(logNumber + 1)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the Persistence n times. */
        verify(mockPersistence, times(logNumber)).deleteLogs(any(String.class), any(String.class));

        /* Verify timer. */
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void errorLogDiscarded() {
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mock(Persistence.class), mock(Ingestion.class), mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);
        channel.setEnabled(false);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        verify(mockListener).onFailure(any(Log.class), any(CancellationException.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void suspendWithFailureCallback() {
        Ingestion mockIngestion = mock(Ingestion.class);
        Persistence mockPersistence = mock(Persistence.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.countLogs(anyString())).thenReturn(30);
        when(mockPersistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(10));
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class)))
                /* Simulate waiting for response for the first batch. */
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        return null;
                    }
                })
                /* Simulate waiting for response for the second batch. */
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        return null;
                    }
                })
                /* Simulate mockIngestion failure for the third batch. */
                .then(getSendAsyncAnswer(new HttpException(404)));

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);

        /* 30 from countLogs and 10 new logs from getLogs. */
        verify(mockListener, times(40)).onBeforeSending(any(Log.class));
        verify(mockListener, times(40)).onFailure(any(Log.class), any(SocketException.class));
        assertFalse(channel.isEnabled());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void suspendWithoutFailureCallback() {
        Ingestion mockIngestion = mock(Ingestion.class);
        Persistence mockPersistence = mock(Persistence.class);

        when(mockPersistence.countLogs(anyString())).thenReturn(3);
        when(mockPersistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(1));
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class)))
                /* Simulate waiting for response for the first batch. */
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        return null;
                    }
                })
                /* Simulate waiting for response for the second batch. */
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        return null;
                    }
                })
                /* Simulate mockIngestion failure for the third batch. */
                .then(getSendAsyncAnswer(new SocketException()));

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        assertFalse(channel.isEnabled());
    }

    @Test
    public void enqueuePersistenceFailure() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);

        /* Simulate Persistence failing. */
        doThrow(new Persistence.PersistenceException("mock", new IOException("mock"))).
                when(mockPersistence).putLog(anyString(), any(Log.class));
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Verify no request is sent if Persistence fails. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setEnabled() throws IOException {

        /* Send a log. */
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(1));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, ingestion, mAppCenterHandler);
        Channel.Listener listener = spy(new AbstractChannelListener());
        channel.addListener(listener);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));

        /* Disable before timer is triggered. */
        channel.setEnabled(false);
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));
        verify(ingestion).close();
        verify(persistence).deleteLogs(TEST_GROUP);
        verify(ingestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(listener).onGloballyEnabled(false);

        /* Enable and send a new log. */
        AtomicReference<Runnable> runnable = catchPostRunnable();
        channel.setEnabled(true);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion).reopen();
        verify(ingestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(listener).onGloballyEnabled(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void disableBeforeCheckingPendingLogs() {
        Ingestion ingestion = mock(Ingestion.class);
        Persistence persistence = mock(Persistence.class);
        final DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, ingestion, mAppCenterHandler);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(1));
        when(ingestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).thenAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {

                /* Simulate a service disabled in the middle of network transaction. */
                ServiceCallback callback = (ServiceCallback) invocation.getArguments()[3];
                channel.removeGroup(TEST_GROUP);
                callback.onCallSucceeded("");
                return null;
            }
        });
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        channel.enqueue(mock(Log.class), TEST_GROUP);

        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogs() throws IOException {
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(3);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(3));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, ingestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        verify(ingestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(3, channel.getCounter(TEST_GROUP));
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogsMoreThan1Batch() throws IOException {
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(103);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(50)).thenAnswer(getGetLogsAnswer(50)).thenAnswer(getGetLogsAnswer(3));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, ingestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        verify(ingestion, times(2)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(3, channel.getCounter(TEST_GROUP));
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion, times(3)).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogsThenDisable() throws IOException {
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(3);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(3));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, ingestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        assertEquals(3, channel.getCounter(TEST_GROUP));
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        channel.setEnabled(false);
        verify(mAppCenterHandler).removeCallbacks(any(Runnable.class));
        verify(ingestion).close();
        verify(persistence).deleteLogs(TEST_GROUP);
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void packageManagerIsBroken() throws Persistence.PersistenceException, DeviceInfoHelper.DeviceInfoException {

        /* Setup mocking to make device properties generation fail. */
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenThrow(new DeviceInfoHelper.DeviceInfoException("mock", new PackageManager.NameNotFoundException()));
        Persistence persistence = mock(Persistence.class);

        @SuppressWarnings("ConstantConditions")
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, persistence, mock(AppCenterIngestion.class), mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        Channel.Listener listener = mock(Channel.Listener.class);
        channel.addListener(listener);

        /* Enqueue a log: listener is called before but then attaching device properties fails before saving the log. */
        Log log = mock(Log.class);
        channel.enqueue(log, TEST_GROUP);
        verify(listener).onPreparingLog(log, TEST_GROUP);
        verify(listener, never()).shouldFilter(log);
        verify(persistence, never()).putLog(TEST_GROUP, log);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void somehowDatabaseEmptiedAfterTimer() throws IOException {

        /* Cover the if (batchId != null) test though it could happen only if the database content disappear after the timer... */
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(2);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), persistence, ingestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);
        verify(ingestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(2, channel.getCounter(TEST_GROUP));
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mAppCenterHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mAppCenterHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void invokeCallbacksAfterSuspendFatal() {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(eq(TEST_GROUP), anyInt(), any(ArrayList.class)))
                .then(getGetLogsAnswer(1))
                /* Logs from here will be used TEST_GROUP to clear pending states. */
                .then(getGetLogsAnswer(DefaultChannel.CLEAR_BATCH_SIZE))
                .then(getGetLogsAnswer(DefaultChannel.CLEAR_BATCH_SIZE - 1))
                /* Logs from here will be used another group to skip callbacks. */
                .then(getGetLogsAnswer(DefaultChannel.CLEAR_BATCH_SIZE));
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new HttpException(403)));

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);
        channel.addGroup(TEST_GROUP + "2", 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Enqueuing 1 event. */
        channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify callbacks invoked (1 + DefaultChannel.CLEAR_BATCH_SIZE + DefaultChannel.CLEAR_BATCH_SIZE - 1) times. */
        verify(mockListener, times(DefaultChannel.CLEAR_BATCH_SIZE * 2)).onBeforeSending(any(Log.class));
        verify(mockListener, times(DefaultChannel.CLEAR_BATCH_SIZE * 2)).onFailure(any(Log.class), any(Exception.class));

        /* Verify logs were deleted. */
        verify(mockPersistence).deleteLogs(TEST_GROUP);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void invokeCallbacksAfterSuspendFatalNoListener() {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        /* Simulate a lot of logs already in database. */
        when(mockPersistence.getLogs(eq(TEST_GROUP), anyInt(), any(ArrayList.class)))
                .then(getGetLogsAnswer(1))
                .then(getGetLogsAnswer(1))
                .then(getGetLogsAnswer(DefaultChannel.CLEAR_BATCH_SIZE));

        /* Make first call hang, and the second call return a fatal error. */
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).thenReturn(null).then(getSendAsyncAnswer(new HttpException(403)));

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, 1, MAX_PARALLEL_BATCHES, null, null);
        channel.addGroup(TEST_GROUP + "2", 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);

        /* Enqueuing 2 events. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify callbacks not invoked. */
        verify(mockListener, never()).onBeforeSending(any(Log.class));
        verify(mockListener, never()).onFailure(any(Log.class), any(Exception.class));

        /* Verify logs were deleted. */
        verify(mockPersistence).deleteLogs(TEST_GROUP);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void invokeCallbacksAfterSuspendRecoverable() {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(eq(TEST_GROUP), anyInt(), any(ArrayList.class)))
                .then(getGetLogsAnswer(1));
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new HttpException(503)));

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);
        channel.addGroup(TEST_GROUP + "2", 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, null);

        /* Enqueuing 1 event. */
        channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify callbacks invoked only for the first log. */
        verify(mockListener).onBeforeSending(any(Log.class));

        /* Verify no failure forwarded. */
        verify(mockListener, never()).onFailure(any(Log.class), any(Exception.class));

        /* Verify no log was deleted. */
        verify(mockPersistence, never()).deleteLogs(TEST_GROUP);

        /* But that we cleared batch state. */
        verify(mockPersistence).clearPendingLogState();
    }
}
