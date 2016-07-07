package avalanche.base.channel;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AvalancheChannelTest {
    private static Context sContext;

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);

        sContext = InstrumentationRegistry.getTargetContext();
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

        final DeviceLog deviceLog = new DeviceLog();
        deviceLog.setSid(UUID.randomUUID());
        deviceLog.setSdkVersion("1.2.3");
        deviceLog.setModel("S5");
        deviceLog.setOemName("HTC");
        deviceLog.setOsName("Android");
        deviceLog.setOsVersion("4.0.3");
        deviceLog.setOsApiLevel(15);
        deviceLog.setLocale("en_US");
        deviceLog.setTimeZoneOffset(120);
        deviceLog.setScreenSize("800x600");
        deviceLog.setAppVersion("3.2.1");
        deviceLog.setAppBuild("42");

        DefaultAvalanchePersistence mockPersistence = mock(DefaultAvalanchePersistence.class);

        //Stubbing getLogs so Persistence returns a batchID and adds 5 logs to the list
        when(mockPersistence.getLogs(any(String.class), anyInt(), any(ArrayList.class))).then(new Answer<String>() {
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[2] instanceof ArrayList) {
                    ArrayList logs = (ArrayList) args[2];
                    logs.add(deviceLog);
                    logs.add(deviceLog);
                    logs.add(deviceLog);
                    logs.add(deviceLog);
                    logs.add(deviceLog);
                }
                return UUID.randomUUID().toString();
            }
        });

        sut.setPersistence(mockPersistence);


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

        sut.setIngestion(mockIngestion);

        //Enqueuing 4 events.
        sut.enqueue(deviceLog, ANALYTICS_GROUP);
        sut.enqueue(deviceLog, ANALYTICS_GROUP);
        sut.enqueue(deviceLog, ANALYTICS_GROUP);
        sut.enqueue(deviceLog, ANALYTICS_GROUP);

        //Check if our counter is equal the number of events.
        assertEquals(4, sut.getAnalyticsCounter());

        //Enqueue another event.
        sut.enqueue(deviceLog, ANALYTICS_GROUP);

        //The counter should have been reset now.
        assertEquals(0, sut.getAnalyticsCounter());

        //Verifying that 5 items have been persisted.
        verify(mockPersistence, times(5)).putLog(ANALYTICS_GROUP, deviceLog);
        ;

        //Verify that we have called the persistence to get 1 batch.
        verify(mockPersistence, times(1)).getLogs(any(String.class), anyInt(), any(ArrayList.class));

        //Verify that we have just one batchId
        assertEquals(1, sut.getAnalyticsBatchIds().size());
    }

    @Test
    public void synchronize() {

    }

    @Test
    public void setDisabled() {

    }
}
