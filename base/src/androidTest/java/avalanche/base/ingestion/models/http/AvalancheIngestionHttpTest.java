package avalanche.base.ingestion.models.http;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

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

import avalanche.base.ingestion.HttpException;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.http.UrlConnectionFactory;
import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.DefaultLogContainerSerializer;
import avalanche.base.ingestion.models.json.LogContainerSerializer;
import avalanche.base.utils.AvalancheLog;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        LogContainerSerializer serializer = new DefaultLogContainerSerializer();
        httpClient.setLogContainerSerializer(serializer);
        httpClient.setUrlConnectionFactory(urlConnectionFactory);

        /* Test calling code. */
        String appId = "app000000";
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        httpClient.sendAsync(appId, installId, container, serviceCallback);
        verify(serviceCallback, timeout(100)).success();
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestProperty("App-ID", appId);
        verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        Assert.assertEquals(serializer.serialize(container), buffer.toString("UTF-8"));
        verify(urlConnection).disconnect();
    }

    @Test
    public void error503() throws JSONException, InterruptedException, IOException {

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
        when(urlConnection.getResponseCode()).thenReturn(503);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Busy".getBytes()));

        /* Configure API client. */
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        LogContainerSerializer serializer = new DefaultLogContainerSerializer();
        httpClient.setLogContainerSerializer(serializer);
        httpClient.setUrlConnectionFactory(urlConnectionFactory);

        /* Test calling code. */
        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        String appId = "app000000";
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        httpClient.sendAsync(appId, installId, container, serviceCallback);
        verify(serviceCallback, timeout(100)).failure(new HttpException(503));
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).disconnect();
    }

    @Test
    public void cancel() throws JSONException, InterruptedException, IOException {
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        httpClient.setUrlConnectionFactory(mock(UrlConnectionFactory.class));
        httpClient.setLogContainerSerializer(new DefaultLogContainerSerializer());
        final Semaphore semaphore = new Semaphore(0);
        ServiceCall call = httpClient.sendAsync("app000000", UUID.randomUUID(), new LogContainer(), new ServiceCallback() {

            @Override
            public void success() {
                semaphore.release();
            }

            @Override
            public void failure(Throwable t) {
                semaphore.release();
            }
        });
        call.cancel();
        Assert.assertFalse(semaphore.tryAcquire(1, TimeUnit.SECONDS));

        /* Calling cancel a second time should be allowed. */
        call.cancel();
    }

    @Test(expected = IllegalStateException.class)
    public void noUrl() {
        new AvalancheIngestionHttp().sendAsync("app000000", UUID.randomUUID(), new LogContainer(), mock(ServiceCallback.class));
    }

    @Test(expected = IllegalStateException.class)
    public void noUrlFactory() {
        AvalancheIngestionHttp http = new AvalancheIngestionHttp();
        http.setBaseUrl("");
        http.sendAsync("app000000", UUID.randomUUID(), new LogContainer(), mock(ServiceCallback.class));
    }


    @Test(expected = IllegalStateException.class)
    public void noSerializer() {
        AvalancheIngestionHttp http = new AvalancheIngestionHttp();
        http.setBaseUrl("");
        http.setUrlConnectionFactory(mock(UrlConnectionFactory.class));
        http.sendAsync("app000000", UUID.randomUUID(), new LogContainer(), mock(ServiceCallback.class));
    }

    @Test
    public void failedConnection() throws IOException {
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        httpClient.setLogContainerSerializer(new DefaultLogContainerSerializer());
        UrlConnectionFactory urlConnectionFactory = mock(UrlConnectionFactory.class);
        httpClient.setUrlConnectionFactory(urlConnectionFactory);
        IOException exception = new IOException("mock");
        when(urlConnectionFactory.openConnection(any(URL.class))).thenThrow(exception);
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        httpClient.sendAsync("app000000", UUID.randomUUID(), new LogContainer(), serviceCallback);
        verify(serviceCallback, timeout(1000)).failure(exception);
        verifyNoMoreInteractions(serviceCallback);
    }
}