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

import static avalanche.core.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;
import static avalanche.core.channel.DefaultAvalancheChannel.ERROR_GROUP;
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

public class DefaultAvalancheChannelTest {
    private static Log sDeviceLog;
    private static LogSerializer sLogSerializer;
    private static Context sContext;

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);

        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);

        sDeviceLog = new MockLog();
        sDeviceLog.setSid(UUIDUtils.randomUUID());
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
        sDeviceLog.setDevice(device);
        sLogSerializer = new DefaultLogSerializer();
        sLogSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
    }

    @Test
    public void creationWorks() {
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        assertNotNull(sut);
    }

    @Test
    public void persistAnalytics() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);
        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

        //don't provide a UUID to prevent sending
        @SuppressWarnings("ConstantConditions")
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, null, mockIngestion, mockPersistence, sLogSerializer);

        //Enqueuing 49 events.
        for (int i = 0; i < 49; i++) {
            sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
            assertTrue(System.currentTimeMillis() - sDeviceLog.getToffset() <= 100);
        }

        //Check if our counter is equal the number of events.
        assertEquals(49, sut.getCounter(ANALYTICS_GROUP));

        //Enqueue another event.
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);

        //The counter should be 0 as we reset the counter after reaching the limit of 50.
        assertEquals(0, sut.getCounter(ANALYTICS_GROUP));

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(50)).putLog(ANALYTICS_GROUP, sDeviceLog);
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
                        logs.add(sDeviceLog);
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

        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        sut.setPersistence(mockPersistence);
        sut.setIngestion(mockIngestion);

        //Enqueuing 49 events.
        for (int i = 0; i < 50; i++) {
            sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        }

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(50)).putLog(ANALYTICS_GROUP, sDeviceLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(mockPersistence, times(1)).deleteLog(any(String.class), any(String.class));

        //The counter should be 0 now as we sent data.
        assertEquals(0, sut.getCounter(ANALYTICS_GROUP));
    }

    @Test
    public void analyticsRecoverable() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds 5 logs to the list for ANALYTICS_GROUP and nothing for ERROR_GROUP
        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String uuidString = null;
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ANALYTICS_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            int size = (int) args[1];
                            for (int i = 0; i < size; i++) {
                                logs.add(sDeviceLog);
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
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);

        //Enqueuing 50 events.
        for (int i = 0; i < 50; i++) {
            sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        }

        //Verifying that 50 items have been persisted.
        verify(mockPersistence, times(50)).putLog(ANALYTICS_GROUP, sDeviceLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have not called deleteLogs on the persistence
        verify(mockPersistence, times(0)).deleteLog(any(String.class), any(String.class));

        //Verify that the Channel is disabled
        assertFalse(sut.isEnabled());

        //Enqueuing 20 more events.
        for (int i = 0; i < 20; i++) {
            sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        }

        //The counter should have been 0 now as we are disabled and the counter is not increased.
        assertEquals(0, sut.getCounter(ANALYTICS_GROUP));

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

        sut.setIngestion(newIngestion);

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
                        logs.add(sDeviceLog);
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
                        logs.add(sDeviceLog);
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
                        logs.add(sDeviceLog);
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

        sut.setPersistence(newPersistence);

        sut.setEnabled(true);
        sut.triggerIngestion();

        //The counter should back to 0 now.
        assertEquals(0, sut.getCounter(ANALYTICS_GROUP));

        //Verify that we have called sendAsync on the ingestion 5 times total.
        verify(newIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(newPersistence, times(3)).deleteLog(any(String.class), any(String.class));
    }

    @Test
    public void analyticsFatal() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds 5 logs to the list for ANALYTICS_GROUP and nothing for ERROR_GROUP
        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String uuidString = null;
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ANALYTICS_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            int size = (int) args[1];
                            for (int i = 0; i < size; i++) {
                                logs.add(sDeviceLog);
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

        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);

        //Enqueuing 50 events.
        for (int i = 0; i < 50; i++) {
            sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        }

        //Verifying that 50 items have been persisted.
        verify(mockPersistence, times(50)).putLog(ANALYTICS_GROUP, sDeviceLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have deleted the failed batch
        verify(mockPersistence, times(1)).deleteLog(any(String.class), any(String.class));

        //Verify that the Channel is disabled
        assertFalse(sut.isEnabled());

        //Enqueuing 20 more events.
        for (int i = 0; i < 20; i++) {
            sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        }

        //The counter should have been 0 now as we are disabled and the counter is not increased.
        assertEquals(0, sut.getCounter(ANALYTICS_GROUP));

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

        sut.setIngestion(newIngestion);

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
                        logs.add(sDeviceLog);
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
                        logs.add(sDeviceLog);
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
                        logs.add(sDeviceLog);
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

        sut.setPersistence(newPersistence);

        sut.setEnabled(true);
        sut.triggerIngestion();

        //The counter should back to 0 now.
        assertEquals(0, sut.getCounter(ANALYTICS_GROUP));

        //Verify that we have called sendAsync on the ingestion 5 times total.
        verify(newIngestion, times(3)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(newPersistence, times(3)).deleteLog(any(String.class), any(String.class));
    }

    @Test
    public void persistErrorLog() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        AvalancheIngestionHttp mockIngestion = mock(AvalancheIngestionHttp.class);

        //don't provide a UUID to prevent sending
        @SuppressWarnings("ConstantConditions")
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, null, mockIngestion, mockPersistence, sLogSerializer);

        //Enqueuing 4 events.
        for (int i = 0; i < 4; i++) {
            sut.enqueue(sDeviceLog, ERROR_GROUP);
            assertTrue(System.currentTimeMillis() - sDeviceLog.getToffset() <= 100);
        }

        //The counter should have been 0 now as we have reached the limit
        assertEquals(0, sut.getCounter(ERROR_GROUP));

        //Enqueue another event.
        sut.enqueue(sDeviceLog, ERROR_GROUP);

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(5)).putLog(ERROR_GROUP, sDeviceLog);
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
                    logs.add(sDeviceLog);
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

        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), sLogSerializer);
        sut.setIngestion(mockIngestion);
        sut.setPersistence(mockPersistence);

        //Enqueuing 1 error log.
        sut.enqueue(sDeviceLog, ERROR_GROUP);

        //Verifying that 1 item have been persisted.
        verify(mockPersistence, times(1)).putLog(ERROR_GROUP, sDeviceLog);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(mockPersistence, times(1)).deleteLog(any(String.class), any(String.class));

        //The counter should be 0 now as we sent data.
        assertEquals(0, sut.getCounter(ERROR_GROUP));
    }

    @Test
    public void errorLogRecoverable() throws AvalanchePersistence.PersistenceException {
        AvalanchePersistence mockPersistence = mock(AvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds 1 log to the list for ERROR_GROUP and nothing for ANALYTICS_GROUP
        //noinspection unchecked
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String uuidString = null;
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ERROR_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sDeviceLog);
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

        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);

        //Enqueuing 1 error.
        sut.enqueue(sDeviceLog, ERROR_GROUP);

        //Verifying that 1 items have been persisted.
        verify(mockPersistence, times(1)).putLog(ERROR_GROUP, sDeviceLog);

        //Verify that we have called sendAsync on the ingestion once for the first item, but not more than that.
        verify(mockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have not called deleteLogs on the persistence
        verify(mockPersistence, times(0)).deleteLog(any(String.class), any(String.class));

        //Verify that the Channel is disabled
        assertFalse(sut.isEnabled());

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

        sut.setIngestion(newIngestion);

        AvalanchePersistence newPersistence = mock(AvalanchePersistence.class);
        //Stubbing getLogs so Persistence returns a batchID and adds 1 log to the list for ERROR_GROUP and nothing for ANALYTICS_GROUP
        //NOTE: the newIngestion-mock responds 6x with something. We only want 5 error logs, so why have 6 then-clauses?
        // because triggerIngestion() will also trigger one request to the persistence for a analytics log, so we need to
        // stub a total of 6 calls until the mocked persistence returns null.
        //noinspection unchecked
        when(newPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ERROR_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sDeviceLog);
                        }
                        return UUIDUtils.randomUUID().toString();
                    }
                }
                return null;
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ERROR_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sDeviceLog);
                        }
                        return UUIDUtils.randomUUID().toString();
                    }
                }
                return null;
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ERROR_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sDeviceLog);
                        }
                        return UUIDUtils.randomUUID().toString();
                    }
                }
                return null;
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ERROR_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sDeviceLog);
                        }
                        return UUIDUtils.randomUUID().toString();
                    }
                }
                return null;
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ERROR_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sDeviceLog);
                        }
                        return UUIDUtils.randomUUID().toString();
                    }
                }
                return null;
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] instanceof String) {
                    if ((args[0]).equals(ERROR_GROUP)) {
                        if (args[2] instanceof ArrayList) {
                            ArrayList logs = (ArrayList) args[2];
                            logs.add(sDeviceLog);
                        }
                        return UUIDUtils.randomUUID().toString();
                    }
                }
                return null;
            }
        }).then(new Answer<String>() {
            @SuppressWarnings("unchecked")
            public String answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        });

        sut.setPersistence(newPersistence);

        sut.setEnabled(true);
        sut.triggerIngestion();


        //Verify that we have called sendAsync on the ingestion 5 times total
        verify(newIngestion, times(5)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence 5 times
        verify(newPersistence, times(5)).deleteLog(any(String.class), any(String.class));
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
                        logs.add(sDeviceLog);
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

        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), mockIngestion, mockPersistence, sLogSerializer);

        //Enqueuing 20 events.
        for (int i = 0; i < 20; i++) {
            sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        }

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(20)).putLog(ANALYTICS_GROUP, sDeviceLog);

        Thread.sleep(4000);

        //Verify that we have called sendAsync on the ingestion
        verify(mockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(mockPersistence, times(1)).deleteLog(any(String.class), any(String.class));

        //The counter should be 0 now as we sent data.
        assertEquals(0, sut.getCounter(ANALYTICS_GROUP));
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
                        logs.add(sDeviceLog);
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
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUIDUtils.randomUUID(), ingestion, persistence, sLogSerializer);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.setEnabled(false);
        verify(ingestion).close();
        verify(persistence).clear();
        Thread.sleep(4000);
        verify(ingestion, never()).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        sut.setEnabled(true);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        Thread.sleep(4000);
        verify(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }
}
