package avalanche.core.channel;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
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
import avalanche.core.persistence.AvalancheDatabasePersistence;
import avalanche.core.persistence.AvalanchePersistence;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.UUIDUtils;

import static avalanche.core.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class DefaultAvalancheChannelTest {

    private final static String TEST_GROUP = "group_test";

    private static Log sMockLog;
    private static LogSerializer sLogSerializer;
    private static Context sContext;

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);

        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);

        sMockLog = new MockLog();
        sMockLog.setSid(UUIDUtils.randomUUID());
        Device device = new Device();
        device.setSdkVersion("1.2.3");
        device.setModel("S5");
        device.setOemName("HTC");
        device.setOsName("Android");
        device.setOsVersion("4.0.3");
        device.setOsApiLevel(15);
        device.setLocale("en_US");
        device.setTimeZoneOffset(120);
        device.setScreenSize("800x600");
        device.setAppVersion("3.2.1");
        device.setAppBuild("42");
        sMockLog.setDevice(device);
        sLogSerializer = new DefaultLogSerializer();
        sLogSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
    }

    @Test
    public void creationWorks() {
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        assertNotNull(channel);
    }

    @Test
    public void persistAnalytics() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

        //don't provide a UUID to prevent sending
        @SuppressWarnings("ConstantConditions")
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, null, mockIngestion, mockPersistence, sLogSerializer);
        channel.addGroup(TEST_GROUP, 50, 3000, 3, null);

        //Enqueuing 49 events.
        for (int i = 0; i < 49; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
            assertTrue(System.currentTimeMillis() - sMockLog.getToffset() <= 100);
        }

        //Check if our counter is equal the number of events.
        assertEquals(49, channel.getCounter(TEST_GROUP));

        //Enqueue another event.
        channel.enqueue(sMockLog, TEST_GROUP);

        //The counter should be 0 as we reset the counter after reaching the limit of 50.
        assertEquals(0, channel.getCounter(TEST_GROUP));

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(50)).putLog(TEST_GROUP, sMockLog);
    }

    @Test
    public void analyticsSuccess() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds N logs to the list, return null for all other calls
        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        });

        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).success();
                }
                return null;
            }
        });

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        AvalancheChannel.Listener mockListener = mock(AvalancheChannel.Listener.class);
        channel.addGroup(TEST_GROUP, 50, 3000, 3, mockListener);

        channel.setPersistence(mockPersistence);
        channel.setIngestion(mockIngestion);

        //Enqueuing 49 events.
        for (int i = 0; i < 50; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
        }

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(50)).putLog(TEST_GROUP, sMockLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        //Verify that we have called onSuccess in the listener
        verify(mockListener, times(50)).onSuccess(any(Log.class));

        //The counter should be 0 now as we sent data.
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @Test
    public void maxRequests() throws AvalanchePersistence.PersistenceException {

        /* Mock database: if N logs are requested, return N mock logs. */
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof Collection) {
                    Collection logs = (Collection) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        });

        /* Mock ingestion: store callbacks so that we choose when to call back later. */
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);
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
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        channel.addGroup(TEST_GROUP, 50, 3000, 3, null);

        channel.setPersistence(mockPersistence);
        channel.setIngestion(mockIngestion);

        /* Enqueue enough logs to be split in N + 1 maximum requests. */
        for (int i = 0; i < 200; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
        }

        /* Verify all logs stored, N requests sent, not log deleted yet. */
        verify(mockPersistence, times(200)).putLog(TEST_GROUP, sMockLog);
        verify(mockIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        /* Make 1 of the call succeed. Verify log deleted. */
        callbacks.get(0).success();
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        /* The request N+1 is now unlocked. */
        verify(mockIngestion, times(4)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Unlock all requests and check logs deleted. */
        for (int i = 1; i < 4; i++)
            callbacks.get(i).success();
        verify(mockPersistence, times(4)).deleteLogs(any(String.class), any(String.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @Test
    public void analyticsRecoverable() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String uuidString = null;
                if (args[0] instanceof String) {
                    if ((args[0]).equals(TEST_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            int size = (int) args[1];
                            for (int i = 0; i < size; i++) {
                                logs.add(sMockLog);
                            }
                        }
                        uuidString = UUIDUtils.randomUUID().toString();
                    }

                }
                return uuidString;
            }
        });

        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);


        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).failure(new SocketException());
                }
                return null;
            }
        });

        //don't provide a UUID to prevent sending
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);
        AvalancheChannel.Listener mockListener = mock(AvalancheChannel.Listener.class);
        channel.addGroup(TEST_GROUP, 50, 3000, 3, mockListener);

        //Enqueuing 50 events.
        for (int i = 0; i < 50; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
        }

        //Verifying that 50 items have been persisted.
        verify(mockPersistence, times(50)).putLog(TEST_GROUP, sMockLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have not called deleteLogs on the persistence
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        //Verify that the Channel is disabled
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).clear();

        //Enqueuing 20 more events.
        for (int i = 0; i < 20; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
        }

        //The counter should have been 0 now as we are disabled and the counter is not increased.
        assertEquals(0, channel.getCounter(TEST_GROUP));

        //Using a fresh ingestion object to change our stub to use the analyticsSuccess()-callback
        AvalancheIngestionHttp newIngestion = mock(AvalancheIngestionHttp.class);
        when(newIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).success();
                }
                return null;
            }
        });

        channel.setIngestion(newIngestion);

        //Use a fresh persistence, that will return 50 objects, then another 20 objects.
        AvalancheDatabasePersistence newPersistence = mock(AvalancheDatabasePersistence.class);
        //noinspection unchecked
        when(newPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    for (int i = 0; i < 25; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        });

        channel.setPersistence(newPersistence);

        channel.setEnabled(true);
        channel.triggerIngestion();

        //The counter should back to 0 now.
        assertEquals(0, channel.getCounter(TEST_GROUP));

        //Verify that we have called sendAsync on the ingestion 5 times total.
        verify(newIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(newPersistence, times(3)).deleteLogs(any(String.class), any(String.class));

        //Verify that we have called onSuccess in the listener
        verify(mockListener, times(50)).onFailure(any(Log.class), any(Exception.class));
    }

    @Test
    public void analyticsFatal() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String uuidString = null;
                if (args[0] instanceof String) {
                    if ((args[0]).equals(TEST_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            int size = (int) args[1];
                            for (int i = 0; i < size; i++) {
                                logs.add(sMockLog);
                            }
                        }
                        uuidString = UUIDUtils.randomUUID().toString();
                    }
                }
                return uuidString;
            }
        });

        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);


        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).failure(new HttpException(403));
                }
                return null;
            }
        });

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);
        channel.addGroup(TEST_GROUP, 50, 3000, 3, null);

        //Enqueuing 50 events.
        for (int i = 0; i < 50; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
        }

        //Verifying that 50 items have been persisted.
        verify(mockPersistence, times(50)).putLog(TEST_GROUP, sMockLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have deleted the failed batch
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        //Verify that the Channel is disabled
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).clear();

        //Enqueuing 20 more events.
        for (int i = 0; i < 20; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
        }

        //The counter should have been 0 now as we are disabled and the counter is not increased.
        assertEquals(0, channel.getCounter(TEST_GROUP));

        //Using a fresh ingestion object to change our stub to use the analyticsSuccess()-callback
        AvalancheIngestionHttp newIngestion = mock(AvalancheIngestionHttp.class);
        when(newIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).success();
                }
                return null;
            }
        });

        channel.setIngestion(newIngestion);

        //Use a fresh persistence, that will return 50 objects, then another 20 objects.
        AvalancheDatabasePersistence newPersistence = mock(AvalancheDatabasePersistence.class);
        //noinspection unchecked
        when(newPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    for (int i = 0; i < 25; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        });

        channel.setPersistence(newPersistence);

        channel.setEnabled(true);
        channel.triggerIngestion();

        //The counter should back to 0 now.
        assertEquals(0, channel.getCounter(TEST_GROUP));

        //Verify that we have called sendAsync on the ingestion 5 times total.
        verify(newIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(newPersistence, times(3)).deleteLogs(any(String.class), any(String.class));
    }

    @Test
    public void persistErrorLog() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

        //don't provide a UUID to prevent sending
        @SuppressWarnings("ConstantConditions")
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, null, mockIngestion, mockPersistence, sLogSerializer);
        channel.addGroup(TEST_GROUP, 1, 3000, 3, null);

        //Enqueuing 4 events.
        for (int i = 0; i < 4; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
            assertTrue(System.currentTimeMillis() - sMockLog.getToffset() <= 100);
        }

        //The counter should have been 0 now as we have reached the limit
        assertEquals(0, channel.getCounter(TEST_GROUP));

        //Enqueue another event.
        channel.enqueue(sMockLog, TEST_GROUP);

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(5)).putLog(TEST_GROUP, sMockLog);
    }

    @Test
    public void errorLogSuccess() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds 5 logs to the list
        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    logs.add(sMockLog);
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        });

        AvalancheIngestion mockIngestion = mock(AvalancheIngestion.class);

        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).success();
                }
                return null;
            }
        });

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        AvalancheChannel.Listener mockListener = mock(AvalancheChannel.Listener.class);
        channel.addGroup(TEST_GROUP, 1, 3000, 3, mockListener);

        channel.setIngestion(mockIngestion);
        channel.setPersistence(mockPersistence);

        //Enqueuing 1 error log.
        channel.enqueue(sMockLog, TEST_GROUP);

        //Verifying that 1 item have been persisted.
        verify(mockPersistence).putLog(TEST_GROUP, sMockLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        //Verify that we have called onSuccess in the listener
        verify(mockListener).onSuccess(any(Log.class));

        //The counter should be 0 now as we sent data.
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @Test
    public void errorLogRecoverable() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String uuidString = null;
                if (args[0] instanceof String) {
                    if ((args[0]).equals(TEST_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sMockLog);
                        }
                        uuidString = UUIDUtils.randomUUID().toString();
                    }

                }
                return uuidString;
            }
        });

        AvalancheIngestion mockIngestion = mock(AvalancheIngestion.class);

        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).failure(new SocketException());
                }
                return null;
            }
        });

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);
        AvalancheChannel.Listener mockListener = mock(AvalancheChannel.Listener.class);
        channel.addGroup(TEST_GROUP, 1, 3000, 3, mockListener);

        //Enqueuing 1 error.
        channel.enqueue(sMockLog, TEST_GROUP);

        //Verifying that 1 items have been persisted.
        verify(mockPersistence).putLog(TEST_GROUP, sMockLog);

        //Verify that we have called sendAsync on the ingestion once for the first item, but not more than that.
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have not called deleteLogs on the persistence
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));

        //Verify that we have called onSuccess in the listener
        verify(mockListener).onFailure(any(Log.class), any(Exception.class));

        //Verify that the Channel is disabled
        assertFalse(channel.isEnabled());
        verify(mockPersistence).clearPendingLogState();
        verify(mockPersistence, never()).clear();

        //Using a fresh ingestion object to change our stub to use the analyticsSuccess()-callback
        AvalancheIngestion newIngestion = mock(AvalancheIngestion.class);
        when(newIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).success();
                }
                return null;
            }
        });

        channel.setIngestion(newIngestion);

        AvalanchePersistence newPersistence = mock(AvalanchePersistence.class);
        Answer<String> answer = new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] instanceof String) {
                    if ((args[0]).equals(TEST_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sMockLog);
                        }
                        return UUIDUtils.randomUUID().toString();
                    }
                }
                return null;
            }
        };

        //noinspection unchecked
        when(newPersistence.getLogs(anyString(), anyInt(), any(ArrayList.class)))
                .then(answer).then(answer).then(answer).then(answer).then(answer)
                .then(new Answer<String>() {
                    @SuppressWarnings("unchecked")
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        return null;
                    }
                });

        channel.setPersistence(newPersistence);

        channel.setEnabled(true);
        channel.triggerIngestion();

        //Verify that we have called sendAsync on the ingestion 5 times total
        verify(newIngestion, times(5)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence 5 times
        verify(newPersistence, times(5)).deleteLogs(any(String.class), any(String.class));
    }

    @Test
    public void handlerInvoked() throws AvalanchePersistence.PersistenceException, InterruptedException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds N logs to the list, return null for all other calls
        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        });

        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

        when(mockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).success();
                }
                return null;
            }
        });

        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);
        channel.addGroup(TEST_GROUP, 50, 3000, 3, null);

        //Enqueuing 20 events.
        for (int i = 0; i < 20; i++) {
            channel.enqueue(sMockLog, TEST_GROUP);
        }

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(20)).putLog(TEST_GROUP, sMockLog);

        Thread.sleep(4000);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));

        //The counter should be 0 now as we sent data.
        assertEquals(0, channel.getCounter(TEST_GROUP));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setEnabled() throws IOException, InterruptedException {
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doThrow(new IOException()).when(ingestion).close();
        AvalanchePersistence persistence = mock(AvalanchePersistence.class);
        when(persistence.getLogs(anyString(), anyInt(), anyList())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    int size = (int) args[1];
                    for (int i = 0; i < size; i++) {
                        logs.add(sMockLog);
                    }
                }
                return UUIDUtils.randomUUID().toString();
            }
        }).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                return null;
            }
        });
        DefaultAvalancheChannel channel = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), ingestion, persistence, sLogSerializer);
        channel.addGroup(TEST_GROUP, 50, 3000, 3, null);

        channel.enqueue(sMockLog, TEST_GROUP);
        channel.setEnabled(false);
        verify(ingestion).close();
        verify(persistence).clear();
        Thread.sleep(4000);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        channel.setEnabled(true);
        channel.enqueue(sMockLog, TEST_GROUP);
        Thread.sleep(4000);
        verify(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }
}
