package avalanche.base.ingestion.models.http;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.http.UrlConnectionFactory;
import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.DefaultLogContainerSerializer;
import avalanche.base.utils.AvalancheLog;

import static avalanche.base.TestUtils.TAG;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AvalancheIngestionHttpTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);
    }

    @Test
    public void success() throws JSONException, InterruptedException, IOException {

        /* Build some payload. */
        LogContainer container = new LogContainer();
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
        container.setLogs(logs);

        /* Configure mock HTTP. */
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        UrlConnectionFactory urlConnectionFactory = mock(UrlConnectionFactory.class);
        when(urlConnectionFactory.openConnection(any(URL.class))).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Configure API client. */
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        DefaultLogContainerSerializer serializer = new DefaultLogContainerSerializer();
        httpClient.setLogContainerSerializer(serializer);
        httpClient.setUrlConnectionFactory(urlConnectionFactory);

        /* Test calling code. */
        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        String appId = "app000000";
        UUID installId = UUID.randomUUID();
        httpClient.sendAsync(appId, installId, container, new ServiceCallback() {

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
        semaphore.acquire();
        Assert.assertNull(failure.get());
        Mockito.verify(urlConnection).setRequestProperty("App-ID", appId);
        Mockito.verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        Assert.assertEquals(serializer.serialize(container), buffer.toString("UTF-8"));
        Mockito.verify(urlConnection).disconnect();
    }

    @Test
    public void cancel() throws JSONException, InterruptedException, IOException {
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        httpClient.setUrlConnectionFactory(mock(UrlConnectionFactory.class));
        httpClient.setLogContainerSerializer(new DefaultLogContainerSerializer());
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