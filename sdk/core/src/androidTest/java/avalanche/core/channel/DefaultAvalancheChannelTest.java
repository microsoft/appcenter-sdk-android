package avalanche.core.channel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.http.AvalancheIngestionHttp;
import avalanche.core.ingestion.http.HttpException;
import avalanche.core.ingestion.models.Device;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.ingestion.models.json.MockLog;
import avalanche.core.ingestion.models.json.MockLogFactory;
import avalanche.core.persistence.AvalanchePersistence;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.UUIDUtils;

import static avalanche.core.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class DefaultAvalancheChannelTest {

    private final static String TEST_GROUP = "group_test";

    private static final int BATCH_TIME_INTERVAL = 500;
    private static final int TIME_TO_SLEEP = 600;
    private static final int MAX_PARALLEL_BATCHES = 3;
    private static LogSerializer sLogSerializer;
    private static Context sContext;

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);

        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);

        sLogSerializer = new DefaultLogSerializer();
        sLogSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
    }

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
                                logs.add(new MockLog());
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

    @Test
    public void creationWorks() {
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        assertNotNull(channel);
    }

    @Test
    public void invalidGroup() throws AvalanchePersistence.PersistenceException {
        Log log = mock(Log.class);
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        @SuppressWarnings("ConstantConditions")
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, null, persistence, mock(AvalancheIngestionHttp.class));

        /* Enqueue a log before group is registered = failure. */
        channel.enqueue(log, "invalid");
        verify(log, never()).setDevice(any(Device.class));
        verify(log, never()).setToffset(anyLong());
        verify(persistence, never()).putLog("invalid", log);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsSuccess() throws AvalanchePersistence.PersistenceException, InterruptedException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);
        AvalancheChannel.GroupListener mockListener = mock(AvalancheChannel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(1)).then(getGetLogsAnswer(2));

        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 49 events. */
        for (int i = 1; i <= 49; i++) {
            channel.enqueue(new MockLog(), TEST_GROUP);
            assertEquals(i, channel.getCounter(TEST_GROUP));
        }

        /* Enqueue another event. */
        channel.enqueue(new MockLog(), TEST_GROUP);

        /* The counter should be 0 as we reset the counter after reaching the limit of 50. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify that 5 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(MockLog.class));

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

        /* Schedule only 1 log after that. */
        channel.enqueue(new MockLog(), TEST_GROUP);
        assertEquals(1, channel.getCounter(TEST_GROUP));
        verify(mockPersistence, times(51)).putLog(eq(TEST_GROUP), any(MockLog.class));
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(50)).onSuccess(any(Log.class));
        Thread.sleep(TIME_TO_SLEEP);
        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mockIngestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(51)).onSuccess(any(Log.class));

        /* 2 more timed logs. */
        channel.enqueue(new MockLog(), TEST_GROUP);
        channel.enqueue(new MockLog(), TEST_GROUP);
        assertEquals(2, channel.getCounter(TEST_GROUP));
        verify(mockPersistence, times(53)).putLog(eq(TEST_GROUP), any(MockLog.class));
        verify(mockIngestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(51)).onSuccess(any(Log.class));
        Thread.sleep(TIME_TO_SLEEP);
        assertEquals(0, channel.getCounter(TEST_GROUP));
        verify(mockIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, times(3)).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(53)).onSuccess(any(Log.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void maxRequests() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

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
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Enqueue enough logs to be split in N + 1 maximum requests. */
        for (int i = 0; i < 200; i++) {
            channel.enqueue(new MockLog(), TEST_GROUP);
        }

        /* Verify all logs stored, N requests sent, not log deleted yet. */
        verify(mockPersistence, times(200)).putLog(eq(TEST_GROUP), any(MockLog.class));
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
    public void maxRequestsInitial() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

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
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Enqueue enough logs to be split in N + 1 maximum requests. */
        for (int i = 0; i < 100; i++) {
            channel.enqueue(new MockLog(), TEST_GROUP);
        }

        /* Verify all logs stored, N requests sent, not log deleted yet. */
        verify(mockPersistence, times(100)).putLog(eq(TEST_GROUP), any(MockLog.class));
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
    public void analyticsRecoverable() throws AvalanchePersistence.PersistenceException, InterruptedException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);
        AvalancheChannel.GroupListener mockListener = mock(AvalancheChannel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(20));
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new SocketException())).then(getSendAsyncAnswer());

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 50 events. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(new MockLog(), TEST_GROUP);
        }

        /* Verify that 50 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(MockLog.class));

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
            channel.enqueue(new MockLog(), TEST_GROUP);
        }

        /* The counter keeps being increased. */
        assertEquals(70, channel.getCounter(TEST_GROUP));

        /* Enable channel. */
        channel.setEnabled(true);

        /* Upon enabling, 1st batch of 50 is sent immediately, 20 logs are remaining. */
        assertEquals(20, channel.getCounter(TEST_GROUP));

        /* Wait for timer. */
        Thread.sleep(TIME_TO_SLEEP);

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
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyticsFatal() throws AvalanchePersistence.PersistenceException, InterruptedException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer(50)).then(getGetLogsAnswer(20));
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new HttpException(403))).then(getSendAsyncAnswer());

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Enqueuing 50 events. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(new MockLog(), TEST_GROUP);
        }

        /* Verify that 50 items have been persisted. */
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(MockLog.class));

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
            channel.enqueue(new MockLog(), TEST_GROUP);
        }

        /* The counter should now be 20. */
        assertEquals(20, channel.getCounter(TEST_GROUP));

        /* Enable channel. */
        channel.setEnabled(true);

        /* Wait timer. */
        Thread.sleep(TIME_TO_SLEEP);

        /* The counter should back to 0 now. */
        assertEquals(0, channel.getCounter(TEST_GROUP));

        /* Verify that we have called sendAsync on the ingestion 2 times total: 1 failure then 1 success. */
        verify(mockIngestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the persistence for the batches. */
        verify(mockPersistence, times(2)).deleteLogs(any(String.class), any(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void errorLogSuccess() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestion mockIngestion = mock(AvalancheIngestion.class);
        AvalancheChannel.GroupListener mockListener = mock(AvalancheChannel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 2 error logs. */
        channel.enqueue(new MockLog(), TEST_GROUP);
        channel.enqueue(new MockLog(), TEST_GROUP);

        /* Verify that 2 items have been persisted. */
        verify(mockPersistence, times(2)).putLog(eq(TEST_GROUP), any(MockLog.class));

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
    }

    @Test
    @SuppressWarnings("unchecked")
    public void errorLogRecoverable() throws AvalanchePersistence.PersistenceException, InterruptedException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestion mockIngestion = mock(AvalancheIngestion.class);
        AvalancheChannel.GroupListener mockListener = mock(AvalancheChannel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(getGetLogsAnswer());
        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer(new SocketException())).then(getSendAsyncAnswer());

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mockListener);

        /* Enqueuing 5 errors. */
        for (int i = 0; i < 5; i++)
            channel.enqueue(new MockLog(), TEST_GROUP);

        /* Verify that 5 items have been persisted. */
        verify(mockPersistence, times(5)).putLog(eq(TEST_GROUP), any(MockLog.class));

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

        channel.setEnabled(true);

        /* Verify that we have called sendAsync on the ingestion 6 times total: 1 failure before re-enabling, 5 success after. */
        verify(mockIngestion, times(6)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Verify that we have called deleteLogs on the persistence 5 times. */
        verify(mockPersistence, times(5)).deleteLogs(any(String.class), any(String.class));
    }

    @Test
    public void enqueuePersistenceFailure() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        /* Simulate persistence failing. */
        doThrow(new AvalanchePersistence.PersistenceException("mock", new IOException("mock"))).
                when(mockPersistence).putLog(anyString(), any(Log.class));
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockPersistence, mockIngestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);

        /* Verify no request is sent if persistence fails. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(new MockLog(), TEST_GROUP);
        }
        verify(mockPersistence, times(50)).putLog(eq(TEST_GROUP), any(MockLog.class));
        verify(mockIngestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setEnabled() throws IOException, InterruptedException {
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doThrow(new IOException()).when(ingestion).close();
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer());
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        channel.enqueue(new MockLog(), TEST_GROUP);
        channel.setEnabled(false);
        verify(ingestion).close();
        verify(persistence).clear();
        Thread.sleep(TIME_TO_SLEEP);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        channel.setEnabled(true);
        channel.enqueue(new MockLog(), TEST_GROUP);
        Thread.sleep(TIME_TO_SLEEP);
        verify(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogs() throws IOException, InterruptedException {
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doThrow(new IOException()).when(ingestion).close();
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        when(persistence.countLogs(anyString())).thenReturn(3);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(3));
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(3, channel.getCounter(TEST_GROUP));
        Thread.sleep(TIME_TO_SLEEP);
        verify(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogsMoreThan1Batch() throws IOException, InterruptedException {
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doThrow(new IOException()).when(ingestion).close();
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        when(persistence.countLogs(anyString())).thenReturn(103);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(50)).thenAnswer(getGetLogsAnswer(50)).thenAnswer(getGetLogsAnswer(3));
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        verify(ingestion, times(2)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(3, channel.getCounter(TEST_GROUP));
        Thread.sleep(TIME_TO_SLEEP);
        verify(ingestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialLogsThenDisable() throws IOException, InterruptedException {
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doThrow(new IOException()).when(ingestion).close();
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        when(persistence.countLogs(anyString())).thenReturn(3);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(getGetLogsAnswer(3));
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        assertEquals(3, channel.getCounter(TEST_GROUP));
        channel.setEnabled(false);
        verify(ingestion).close();
        verify(persistence).clear();
        Thread.sleep(TIME_TO_SLEEP);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void listener() throws AvalanchePersistence.PersistenceException {

        @SuppressWarnings("ConstantConditions")
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, null, mock(AvalanchePersistence.class), mock(AvalancheIngestionHttp.class));
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        AvalancheChannel.Listener listener = mock(AvalancheChannel.Listener.class);
        channel.addListener(listener);
        MockLog log = new MockLog();
        channel.enqueue(log, TEST_GROUP);
        verify(listener).onEnqueuingLog(log, TEST_GROUP);

        /* Check no more calls after removing listener. */
        log = new MockLog();
        channel.removeListener(listener);
        channel.enqueue(log, TEST_GROUP);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void packageManagerIsBroken() throws PackageManager.NameNotFoundException, AvalanchePersistence.PersistenceException {

        /* Setup mocking to make device properties generation fail. */
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        //noinspection WrongConstant
        when(packageManager.getPackageInfo(any(String.class), anyInt())).thenThrow(new PackageManager.NameNotFoundException());
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        @SuppressWarnings("ConstantConditions")
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(context, null, persistence, mock(AvalancheIngestionHttp.class));
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        AvalancheChannel.Listener listener = mock(AvalancheChannel.Listener.class);
        channel.addListener(listener);

        /* Enqueue a log: listener is called before but then attaching device properties fails before saving the log. */
        MockLog log = new MockLog();
        channel.enqueue(log, TEST_GROUP);
        verify(listener).onEnqueuingLog(log, TEST_GROUP);
        verify(persistence, never()).putLog(TEST_GROUP, log);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void somehowDatabaseEmptiedAfterTimer() throws IOException, InterruptedException {

        /* Cover the if (batchId != null) test though it could happen only if the database content disappear after the timer... */
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doThrow(new IOException()).when(ingestion).close();
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        when(persistence.countLogs(anyString())).thenReturn(2);
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), persistence, ingestion);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        assertEquals(2, channel.getCounter(TEST_GROUP));
        Thread.sleep(TIME_TO_SLEEP);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }
}
