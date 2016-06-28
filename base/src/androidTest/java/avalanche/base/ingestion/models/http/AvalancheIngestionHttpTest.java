package avalanche.base.ingestion.models.http;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import avalanche.base.ingestion.HttpException;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.DefaultLogContainerSerializer;
import avalanche.base.utils.AvalancheLog;

import static avalanche.base.TestUtils.TAG;

public class AvalancheIngestionHttpTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);
    }

    @Test
    public void deviceLog() throws JSONException, InterruptedException {
        LogContainer expectedContainer = new LogContainer();
        DeviceLog deviceLog = new DeviceLog();
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
        List<Log> logs = new ArrayList<>();
        logs.add(deviceLog);
        expectedContainer.setLogs(logs);

        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("https://puppets.device.mobileengagement-dev.windows-int.net");
        httpClient.setLogContainerSerializer(new DefaultLogContainerSerializer());
        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        httpClient.sendAsync("app000000", UUID.randomUUID(), expectedContainer, new ServiceCallback() {

            @Override
            public void success() {
                AvalancheLog.info(TAG, "Call success");
                semaphore.release();
            }

            @Override
            public void failure(Throwable t) {
                AvalancheLog.error(TAG, "Call failure", t);
                failure.set(t);
                semaphore.release();
            }
        });
        semaphore.tryAcquire(1, TimeUnit.MINUTES);
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable throwable = failure.get();
        Assert.assertTrue(throwable instanceof HttpException);
        HttpException exception = (HttpException) throwable;
        Assert.assertEquals(404, exception.getStatusCode());
    }

    @Test
    public void cancel() throws JSONException, InterruptedException {
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("https://puppets.device.mobileengagement-dev.windows-int.net");
        httpClient.setLogContainerSerializer(new DefaultLogContainerSerializer());
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Semaphore semaphore = new Semaphore(0);
        httpClient.sendAsync("app000000", UUID.randomUUID(), new LogContainer(), new ServiceCallback() {

            @Override
            public void success() {
                AvalancheLog.info(TAG, "Call success");
                semaphore.release();
            }

            @Override
            public void failure(Throwable t) {
                AvalancheLog.error(TAG, "Call failure", t);
                semaphore.release();
            }
        }).cancel();
        Assert.assertFalse(semaphore.tryAcquire(1, TimeUnit.SECONDS));
    }
}