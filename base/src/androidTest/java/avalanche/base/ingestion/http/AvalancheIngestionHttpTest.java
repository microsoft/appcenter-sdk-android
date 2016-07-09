package avalanche.base.ingestion.http;

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

import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.Device;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.DefaultLogSerializer;
import avalanche.base.ingestion.models.json.LogSerializer;
import avalanche.base.ingestion.models.json.MockLog;
import avalanche.base.ingestion.models.json.MockLogFactory;
import avalanche.base.utils.AvalancheLog;

import static avalanche.base.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static org.mockito.Matchers.eq;
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
        Log log = new MockLog();
        log.setDevice(device);
        log.setSid(UUID.randomUUID());
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);

        /* Configure mock HTTP. */
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        UrlConnectionFactory urlConnectionFactory = mock(UrlConnectionFactory.class);
        when(urlConnectionFactory.openConnection(eq(new URL("http://mock/logs?api-version=1.0.0-preview20160708")))).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Configure API client. */
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        httpClient.setLogSerializer(serializer);
        httpClient.setUrlConnectionFactory(urlConnectionFactory);

        /* Test calling code. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        httpClient.sendAsync(appKey, installId, container, serviceCallback);
        verify(serviceCallback, timeout(100)).success();
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestProperty("Content-Type", "application/json");
        verify(urlConnection).setRequestProperty("App-Key", appKey.toString());
        verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        Assert.assertEquals(serializer.serializeContainer(container), buffer.toString("UTF-8"));
        verify(urlConnection).disconnect();
    }

    @Test
    public void error503() throws JSONException, InterruptedException, IOException {

        /* Build some payload. */
        LogContainer container = new LogContainer();
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
        Log log = new MockLog();
        log.setDevice(device);
        log.setSid(UUID.randomUUID());
        List<Log> logs = new ArrayList<>();
        logs.add(log);
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
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        httpClient.setLogSerializer(serializer);
        httpClient.setUrlConnectionFactory(urlConnectionFactory);

        /* Test calling code. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        httpClient.sendAsync(appKey, installId, container, serviceCallback);
        verify(serviceCallback, timeout(100)).failure(new HttpException(503));
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).disconnect();
    }

    @Test
    public void cancel() throws JSONException, InterruptedException, IOException {
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        httpClient.setUrlConnectionFactory(mock(UrlConnectionFactory.class));
        httpClient.setLogSerializer(new DefaultLogSerializer());
        final Semaphore semaphore = new Semaphore(0);
        ServiceCall call = httpClient.sendAsync(UUID.randomUUID(), UUID.randomUUID(), new LogContainer(), new ServiceCallback() {

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
        new AvalancheIngestionHttp().sendAsync(UUID.randomUUID(), UUID.randomUUID(), new LogContainer(), mock(ServiceCallback.class));
    }

    @Test(expected = IllegalStateException.class)
    public void noUrlFactory() {
        AvalancheIngestionHttp http = new AvalancheIngestionHttp();
        http.setBaseUrl("");
        http.sendAsync(UUID.randomUUID(), UUID.randomUUID(), new LogContainer(), mock(ServiceCallback.class));
    }

    @Test(expected = IllegalStateException.class)
    public void noSerializer() {
        AvalancheIngestionHttp http = new AvalancheIngestionHttp();
        http.setBaseUrl("");
        http.setUrlConnectionFactory(mock(UrlConnectionFactory.class));
        http.sendAsync(UUID.randomUUID(), UUID.randomUUID(), new LogContainer(), mock(ServiceCallback.class));
    }

    @Test
    public void failedConnection() throws IOException {
        AvalancheIngestionHttp httpClient = new AvalancheIngestionHttp();
        httpClient.setBaseUrl("http://mock");
        httpClient.setLogSerializer(new DefaultLogSerializer());
        UrlConnectionFactory urlConnectionFactory = mock(UrlConnectionFactory.class);
        httpClient.setUrlConnectionFactory(urlConnectionFactory);
        IOException exception = new IOException("mock");
        when(urlConnectionFactory.openConnection(any(URL.class))).thenThrow(exception);
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        httpClient.sendAsync(UUID.randomUUID(), UUID.randomUUID(), new LogContainer(), serviceCallback);
        verify(serviceCallback, timeout(1000)).failure(exception);
        verifyNoMoreInteractions(serviceCallback);
    }
}