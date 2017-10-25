package com.microsoft.appcenter.ingestion;

import android.content.Context;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.MobileCenterLog;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({IngestionHttp.class, MobileCenterLog.class})
public class IngestionHttpTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void sendAsync() throws Exception {

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log = mock(Log.class);
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);
        LogSerializer serializer = mock(LogSerializer.class);
        when(serializer.serializeContainer(any(LogContainer.class))).thenReturn("mockPayload");

        /* Configure mock HTTP. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });

        /* Test calling code. */
        IngestionHttp ingestionHttp = new IngestionHttp(mock(Context.class), serializer);
        ingestionHttp.setLogUrl("http://mock");
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestionHttp.sendAsync(appSecret, installId, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(IngestionHttp.APP_SECRET, appSecret);
        expectedHeaders.put(IngestionHttp.INSTALL_ID, installId.toString());
        verify(httpClient).callAsync(eq("http://mock" + IngestionHttp.API_PATH), eq(METHOD_POST), eq(expectedHeaders), notNull(HttpClient.CallTemplate.class), eq(serviceCallback));
        assertNotNull(callTemplate.get());
        assertEquals("mockPayload", callTemplate.get().buildRequestBody());

        /* Verify close. */
        ingestionHttp.close();
        verify(httpClient).close();
    }

    @Test
    public void failedSerialization() throws Exception {

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log = mock(Log.class);
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);
        LogSerializer serializer = mock(LogSerializer.class);
        JSONException exception = new JSONException("mock");
        when(serializer.serializeContainer(any(LogContainer.class))).thenThrow(exception);

        /* Configure mock HTTP. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });

        /* Test calling code. */
        IngestionHttp ingestionHttp = new IngestionHttp(mock(Context.class), serializer);
        ingestionHttp.setLogUrl("http://mock");
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestionHttp.sendAsync(appSecret, installId, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(IngestionHttp.APP_SECRET, appSecret);
        expectedHeaders.put(IngestionHttp.INSTALL_ID, installId.toString());
        verify(httpClient).callAsync(eq("http://mock/logs?api_version=1.0.0-preview20160914"), eq(METHOD_POST), eq(expectedHeaders), notNull(HttpClient.CallTemplate.class), eq(serviceCallback));
        assertNotNull(callTemplate.get());

        try {
            callTemplate.get().buildRequestBody();
            Assert.fail("Expected json exception");
        } catch (JSONException ignored) {
        }

        /* Verify close. */
        ingestionHttp.close();
        verify(httpClient).close();
    }

    @Test
    public void onBeforeCalling() throws Exception {

        /* Mock instances. */
        URL url = new URL("http://mock/path/file");
        String appSecret = UUIDUtils.randomUUID().toString();
        String obfuscatedSecret = HttpUtils.hideSecret(appSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put("Another-Header", "Another-Value");
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret);
        MobileCenterLog.setLogLevel(android.util.Log.VERBOSE);
        mockStatic(MobileCenterLog.class);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(url, headers);

        /* Verify url log. */
        verifyStatic();
        MobileCenterLog.verbose(anyString(), contains(url.toString()));

        /* Verify header log. */
        for (Map.Entry<String, String> header : headers.entrySet()) {
            verifyStatic();
            MobileCenterLog.verbose(anyString(), contains(header.getValue()));
        }

        /* Put app secret to header. */
        headers.put(IngestionHttp.APP_SECRET, appSecret);
        callTemplate.onBeforeCalling(url, headers);

        /* Verify app secret is in log. */
        verifyStatic();
        MobileCenterLog.verbose(anyString(), contains(obfuscatedSecret));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onBeforeCallingWithAnotherLogLevel() throws Exception {

        /* Mock instances. */
        String appSecret = UUIDUtils.randomUUID().toString();
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret);

        /* Change log level. */
        MobileCenterLog.setLogLevel(android.util.Log.WARN);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(mock(URL.class), mock(Map.class));

        /* Verify. */
        verifyStatic(never());
        MobileCenterLog.verbose(anyString(), anyString());
    }

    private HttpClient.CallTemplate getCallTemplate(String appSecret) throws Exception {

        /* Configure mock HTTP to get an instance of IngestionCallTemplate. */
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });
        IngestionHttp ingestionHttp = new IngestionHttp(mock(Context.class), mock(LogSerializer.class));
        ingestionHttp.setLogUrl("http://mock");
        assertEquals(call, ingestionHttp.sendAsync(appSecret, UUIDUtils.randomUUID(), mock(LogContainer.class), mock(ServiceCallback.class)));
        return callTemplate.get();
    }
}