package avalanche.base.channel;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.UUID;

import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.persistence.AvalanchePersistence;
import avalanche.base.persistence.DefaultAvalanchePersistence;
import avalanche.base.utils.AvalancheLog;

import static avalanche.base.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AvalancheChannelTest {
    private static Context sContext;
    private static DeviceLog sDeviceLog;
    private static DefaultAvalanchePersistence sMockPersistence;
    private static AvalancheIngestionHttp sMockIngestion;

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);

        sContext = InstrumentationRegistry.getTargetContext();

        sDeviceLog = new DeviceLog();
        sDeviceLog.setSid(UUID.randomUUID());
        sDeviceLog.setSdkVersion("1.2.3");
        sDeviceLog.setModel("S5");
        sDeviceLog.setOemName("HTC");
        sDeviceLog.setOsName("Android");
        sDeviceLog.setOsVersion("4.0.3");
        sDeviceLog.setOsApiLevel(15);
        sDeviceLog.setLocale("en_US");
        sDeviceLog.setTimeZoneOffset(120);
        sDeviceLog.setScreenSize("800x600");
        sDeviceLog.setAppVersion("3.2.1");
        sDeviceLog.setAppBuild("42");

        sMockPersistence = mock(DefaultAvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds 5 logs to the list
        when(sMockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    logs.add(sDeviceLog);
                    logs.add(sDeviceLog);
                    logs.add(sDeviceLog);
                    logs.add(sDeviceLog);
                    logs.add(sDeviceLog);
                }
                return UUID.randomUUID().toString();
            }
        });

        sMockIngestion = mock(AvalancheIngestionHttp.class);
    }

    @Test
    public void creationWorks() {
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUID.randomUUID());
        assertNotNull(sut);
    }

    @Test
    public void enqueueWithoutSending() throws AvalanchePersistence.PersistenceException, InterruptedException {
        //don't provide a UUID to prevent sending
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, null);
        sut.setPersistence(sMockPersistence);
        sut.setIngestion(sMockIngestion);

        //Enqueuing 4 events.
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);

        //Check if our counter is equal the number of events.
        assertEquals(4, sut.getAnalyticsCounter());

        //Enqueue another event.
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);

        //The counter should have been reset now.
        assertEquals(0, sut.getAnalyticsCounter());

        //Verifying that 5 items have been persisted.
        verify(sMockPersistence, times(5)).putLog(ANALYTICS_GROUP, sDeviceLog);
        ;

        //Verify that we have called the persistence to get 1 batch.
        verify(sMockPersistence, times(1)).getLogs(any(String.class), anyInt(), any(ArrayList.class));

        //Verify that we have just one batchId
        assertEquals(1, sut.getAnalyticsBatchIds().size());
    }

    @Test
    public void sendingSuccess() throws AvalanchePersistence.PersistenceException {
        //don't provide a UUID to prevent sending
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUID.randomUUID());
        sut.setPersistence(sMockPersistence);
        sut.setIngestion(sMockIngestion);

        when(sMockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).success();
                }
                return null;
            }
        });

        //Enqueuing 5 events.
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);

        //Verify that we have called sendAsync on the ingestion
        verify(sMockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(sMockPersistence, times(1)).deleteLog(any(String.class), any(String.class));
    }

    @Test
    public void sendingRecoverableError() throws AvalanchePersistence.PersistenceException {
        //don't provide a UUID to prevent sending
        DefaultAvalancheChannel sut = new DefaultAvalancheChannel(sContext, UUID.randomUUID());
        sut.setPersistence(sMockPersistence);
        sut.setIngestion(sMockIngestion);

        when(sMockIngestion.sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[3] instanceof ServiceCallback) {
                    ((ServiceCallback) invocation.getArguments()[3]).failure(new SocketException());
                }
                return null;
            }
        });

        //Enqueuing 5 events.
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);
        sut.enqueue(sDeviceLog, ANALYTICS_GROUP);

        //Verify that we have called sendAsync on the ingestion
        verify(sMockIngestion, times(1)).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        //Verify that we have called deleteLogs on the persistence
        verify(sMockPersistence, times(0)).deleteLog(any(String.class), any(String.class));

        //Verify that the Channel is disabled
        assertTrue(sut.isDisabled());
    }

    @Test
    public void synchronize() {

    }

    @Test
    public void setDisabled() {

    }
}
