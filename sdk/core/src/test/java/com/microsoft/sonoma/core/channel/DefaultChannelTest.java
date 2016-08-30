package com.microsoft.sonoma.core.channel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.microsoft.sonoma.core.ingestion.Ingestion;
import com.microsoft.sonoma.core.ingestion.ServiceCallback;
import com.microsoft.sonoma.core.ingestion.http.HttpException;
import com.microsoft.sonoma.core.ingestion.http.IngestionHttp;
import com.microsoft.sonoma.core.ingestion.models.Device;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.LogContainer;
import com.microsoft.sonoma.core.persistence.Persistence;
import com.microsoft.sonoma.core.utils.DeviceInfoHelper;
import com.microsoft.sonoma.core.utils.IdHelper;
import com.microsoft.sonoma.core.utils.UUIDUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({DefaultChannel.class, IdHelper.class, DeviceInfoHelper.class})
public class DefaultChannelTest {

    private static final String TEST_GROUP = "group_test";
    private static final long BATCH_TIME_INTERVAL = 500;
    private static final int MAX_PARALLEL_BATCHES = 3;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Handler mHandler;

    private static Answer<String> getGetLogsAnswer() {
        return getGetLogsAnswer(0);
    }

    private static Answer<String> getGetLogsAnswer(final int size) {
        return new Answer<String>() {
            @Override
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String uuidString = null;
                if (args[0] instanceof String) {
                    if ((args[0]).equals(TEST_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            int length = size > 0 ? size : (int) args[1];
                            for (int i = 0; i < length; i++) {
                                logs.add(mock(Log.class));
                            }
                        }
                        uuidString = UUIDUtils.randomUUID().toString();
                    }

                }
                return uuidString;
            }
        };
    }

    private static Answer<Object> getSendAsyncAnswer() {
        return getSendAsyncAnswer(null);
    }

    private static Answer<Object> getSendAsyncAnswer(final Exception e) {
        return new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    if (e == null)
                        ((ServiceCallback) invocation.getArguments()[3]).onCallSucceeded();
                    else
                        ((ServiceCallback) invocation.getArguments()[3]).onCallFailed(e);
                }
                return null;
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        mockStatic(IdHelper.class, new Returns(UUIDUtils.randomUUID()));
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenReturn(mock(Device.class));
        whenNew(Handler.class).withAnyArguments().thenReturn(mHandler);
    }

    @Test
    public void invalidGroup() throws Persistence.PersistenceException {
        Persistence persistence = mock(Persistence.class);
        Channel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), persistence, mock(Ingestion.class));

        /* Enqueue a log before group is registered = failure. */
        Log log = mock(Log.class);
        channel.enqueue(log, TEST_GROUP);
        verify(log, never()).setDevice(any(Device.class));
        verify(log, never()).setToffset(anyLong());
        verify(persistence, never()).putLog(TEST_GROUP, log);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsSuccess() throws Persistence.PersistenceException, InterruptedException {
        Persistence mockPersistence = mock(Persistence.class);
        IngestionHttp mockIngestion = mock(IngestionHttp.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(1)).then(getGetLogsAnswer(2));

        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 49 events. */
        for (int i = 1; i <= 49; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
            assertEquals(i, channel.getCounter(TEST_GROUP));
        }
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));

        /* Enqueue another event. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        verify(mHandler).removeCallbacks(any(Runnable.class));

        /* The counter should be 0 as we reset the counter after reaching the limit of 50. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify that 5 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion. */
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the persistence. */
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
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(50)).onSuccess(any(Log.class));

        /* Simulate the timer. */
        assertNotNull(runnable.get());
        runnable.get().run();
        runnable.set(null);

        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mockIngestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(51)).onSuccess(any(Log.class));

        /* 2 more timed logs. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        assertEquals(2, channel.getCounter(TEST_GROUP));
        verify(mockPersistence, times(53)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(51)).onSuccess(any(Log.class));

        /* Simulate the timer. */
        assertNotNull(runnable.get());
        runnable.get().run();

        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mockIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(3)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(53)).onSuccess(any(Log.class));

        /* Check total timers. */
        verify(mHandler, times(3)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler).removeCallbacks(any(Runnable.class));
    }

    @NonNull
    private AtomicReference<Runnable> catchPostRunnable() {
        final AtomicReference<Runnable> runnable = new AtomicReference<>();
        when(mHandler.postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
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
        IngestionHttp mockIngestion = mock(IngestionHttp.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());

        final List<ServiceCallback> callbacks = new ArrayList<>();
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    callbacks.add((ServiceCallback) invocation.getArguments()[3]);
                }
                return null;
            }
        });

        /* Init channel with mocks. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Enqueue enough logs to be split in N + 1 maximum requests. */
        for (int i = 0; i < 200; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mHandler, times(4)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, times(4)).removeCallbacks(any(Runnable.class));

        /* Verify all logs stored, N requests sent, not log deleted yet. */
        verify(mockPersistence, times(200)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Make 1 of the call succeed. Verify log deleted. */
        callbacks.get(0).onCallSucceeded();
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* The request N+1 is now unlocked. */
        verify(mockIngestion, times(4)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Unlock all requests and check logs deleted. */
        for (int i = 1; i < 4; i++)
            callbacks.get(i).onCallSucceeded();
        verify(mockPersistence, times(4)).deleteLogs(any(String.class), any(String.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void maxRequestsInitial() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        IngestionHttp mockIngestion = mock(IngestionHttp.class);

        when(mockPersistence.countLogs(any(String.class))).thenReturn(100);
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());

        final List<ServiceCallback> callbacks = new ArrayList<>();
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    callbacks.add((ServiceCallback) invocation.getArguments()[3]);
                }
                return null;
            }
        });

        /* Init channel with mocks. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Enqueue enough logs to be split in N + 1 maximum requests. */
        for (int i = 0; i < 100; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }

        /* Verify all logs stored, N requests sent, not log deleted yet. */
        verify(mockPersistence, times(100)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Make 1 of the call succeed. Verify log deleted. */
        callbacks.get(0).onCallSucceeded();
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* The request N+1 is now unlocked. */
        verify(mockIngestion, times(4)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Unlock all requests and check logs deleted. */
        for (int i = 1; i < 4; i++)
            callbacks.get(i).onCallSucceeded();
        verify(mockPersistence, times(4)).deleteLogs(any(String.class), any(String.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Only 2 batches after channel start (non initial logs), verify timer interactions. */
        verify(mHandler, times(2)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, times(2)).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsRecoverable() throws Persistence.PersistenceException, InterruptedException {
        Persistence mockPersistence = mock(Persistence.class);
        IngestionHttp mockIngestion = mock(IngestionHttp.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(20));
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new SocketException())).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 50 events. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler).removeCallbacks(any(Runnable.class));

        /* Verify that 50 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion. */
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have not called deleteLogs on the persistence. */
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Verify that the Channel is disabled. */
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).clear();

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
        verify(mockIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the persistence (2 successful batches, the first call was a recoverable failure). */
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));

        /* Verify that we have called onBeforeSending in the listener. getLogs will return 50, 50, 50 and 25. */
        verify(mockListener, times(120)).onBeforeSending(any(Log.class));

        /* Verify that we have called the listener. */
        verify(mockListener, times(50)).onFailure(any(Log.class), any(Exception.class));

        /* Verify timer. */
        verify(mHandler, times(2)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsFatal() throws Persistence.PersistenceException, InterruptedException {
        Persistence mockPersistence = mock(Persistence.class);
        IngestionHttp mockIngestion = mock(IngestionHttp.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(20));
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new HttpException(403))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Enqueuing 50 events. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler).removeCallbacks(any(Runnable.class));

        /* Verify that 50 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion. */
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have deleted the failed batch. */
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* Verify that the Channel is disabled. */
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).clear();

        /* Enqueuing 20 more events. */
        for (int i = 0; i < 20; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }

        /* The counter should now be 20. */
        assertEquals(20, channel.getCounter(TEST_GROUP));

        /* No more timer yet at this point. */
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler).removeCallbacks(any(Runnable.class));

        /* Prepare to mock timer. */
        AtomicReference<Runnable> runnable = catchPostRunnable();

        /* Enable channel. */
        channel.setEnabled(true);

        /* Wait for timer. */
        assertNotNull(runnable.get());
        runnable.get().run();

        /* The counter should back to 0 now. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify that we have called sendAsync on the ingestion 2 times total: 1 failure then 1 success. */
        verify(mockIngestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the persistence for the batches. */
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));

        /* Verify timer. */
        verify(mHandler, times(2)).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void errorLogSuccess() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion mockIngestion = mock(Ingestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 2 error logs. */
        channel.enqueue(mock(Log.class), TEST_GROUP);
        channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify that 2 items have been persisted. */
        verify(mockPersistence, times(2)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion twice as batch size is 1. */
        verify(mockIngestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the persistence. */
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));

        /* Verify that we have called onBeforeSending in the listener. */
        verify(mockListener, times(2)).onBeforeSending(any(Log.class));

        /* Verify that we have called onSuccess in the listener. */
        verify(mockListener, times(2)).onSuccess(any(Log.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify timer. */
        verify(mHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void errorLogRecoverable() throws Persistence.PersistenceException, InterruptedException {
        Persistence mockPersistence = mock(Persistence.class);
        Ingestion mockIngestion = mock(Ingestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new SocketException())).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 5 errors. */
        for (int i = 0; i < 5; i++)
            channel.enqueue(mock(Log.class), TEST_GROUP);

        /* Verify that 5 items have been persisted. */
        verify(mockPersistence, times(5)).putLog(eq(TEST_GROUP), any(Log.class));

        /* Verify that we have called sendAsync on the ingestion once for the first item, but not more than that. */
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have not called deleteLogs on the persistence. */
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Verify that we have called onBeforeSending in the listener. */
        verify(mockListener).onBeforeSending(any(Log.class));

        /* Verify that we have called the listener. */
        verify(mockListener).onFailure(any(Log.class), any(Exception.class));

        /* Verify that the Channel is disabled. */
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).clear();

        /* Verify timer. */
        verify(mHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, never()).removeCallbacks(any(Runnable.class));

        channel.setEnabled(true);

        /* Verify that we have called sendAsync on the ingestion 6 times total: 1 failure before re-enabling, 5 success after. */
        verify(mockIngestion, times(6)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the persistence 5 times. */
        verify(mockPersistence, times(5)).deleteLogs(any(String.class), any(String.class));

        /* Verify timer. */
        verify(mHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void enqueuePersistenceFailure() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);

        /* Simulate persistence failing. */
        doThrow(new Persistence.PersistenceException("mock", new IOException("mock"))).
                when(mockPersistence).putLog(anyString(), any(Log.class));
        IngestionHttp mockIngestion = mock(IngestionHttp.class);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Verify no request is sent if persistence fails. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP);
        }
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(Log.class));
        verify(mockIngestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setEnabled() throws IOException, InterruptedException {

        /* Send a log. */
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer());
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));

        /* Disable before timer is triggered. */
        channel.setEnabled(false);
        verify(mHandler).removeCallbacks(any(Runnable.class));
        verify(ingestion).close();
        verify(persistence).clear();
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Enable and send a new log. */
        AtomicReference<Runnable> runnable = catchPostRunnable();
        channel.setEnabled(true);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogs() throws IOException, InterruptedException {
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(3);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(3));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(3, channel.getCounter(TEST_GROUP));
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogsMoreThan1Batch() throws IOException, InterruptedException {
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(103);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(50)).thenAnswer(getGetLogsAnswer(50)).thenAnswer(getGetLogsAnswer(3));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        verify(ingestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(3, channel.getCounter(TEST_GROUP));
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, never()).removeCallbacks(any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogsThenDisable() throws IOException, InterruptedException {
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(3);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(3));
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        assertEquals(3, channel.getCounter(TEST_GROUP));
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        channel.setEnabled(false);
        verify(mHandler).removeCallbacks(any(Runnable.class));
        verify(ingestion).close();
        verify(persistence).clear();
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void listener() throws Persistence.PersistenceException {

        @SuppressWarnings("ConstantConditions")
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, mock(Persistence.class), mock(IngestionHttp.class));
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        Channel.Listener listener = mock(Channel.Listener.class);
        channel.addListener(listener);
        Log log = mock(Log.class);
        channel.enqueue(log, TEST_GROUP);
        verify(listener).onEnqueuingLog(log, TEST_GROUP);

        /* Check no more calls after removing listener. */
        log = mock(Log.class);
        channel.removeListener(listener);
        channel.enqueue(log, TEST_GROUP);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void packageManagerIsBroken() throws Persistence.PersistenceException, DeviceInfoHelper.DeviceInfoException {

        /* Setup mocking to make device properties generation fail. */
        when(DeviceInfoHelper.getDeviceInfo(any(Context.class))).thenThrow(new DeviceInfoHelper.DeviceInfoException("mock", new PackageManager.NameNotFoundException()));
        Persistence persistence = mock(Persistence.class);
        @SuppressWarnings("ConstantConditions")
        DefaultChannel channel = new DefaultChannel(mock(Context.class), null, persistence, mock(IngestionHttp.class));
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        Channel.Listener listener = mock(Channel.Listener.class);
        channel.addListener(listener);

        /* Enqueue a log: listener is called before but then attaching device properties fails before saving the log. */
        Log log = mock(Log.class);
        channel.enqueue(log, TEST_GROUP);
        verify(listener).onEnqueuingLog(log, TEST_GROUP);
        verify(persistence, never()).putLog(TEST_GROUP, log);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void somehowDatabaseEmptiedAfterTimer() throws IOException, InterruptedException {

        /* Cover the if (batchId != null) test though it could happen only if the database content disappear after the timer... */
        AtomicReference<Runnable> runnable = catchPostRunnable();
        Ingestion ingestion = mock(Ingestion.class);
        doThrow(new IOException()).when(ingestion).close();
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(anyString())).thenReturn(2);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(2, channel.getCounter(TEST_GROUP));
        assertNotNull(runnable.get());
        runnable.get().run();
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mHandler).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mHandler, never()).removeCallbacks(any(Runnable.class));
    }
}
