package com.microsoft.azure.mobile.ingestion;

import android.content.Context;

import com.microsoft.azure.mobile.http.DefaultHttpClient;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.mobile.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest(IngestionHttp.class)
public class IngestionHttpTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void sendAsync() throws Exception {

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log = mock(Log.class);
        long logAbsoluteTime = 123L;
        when(log.getToffset()).thenReturn(logAbsoluteTime);
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);
        LogSerializer serializer = mock(LogSerializer.class);
        when(serializer.serializeContainer(any(LogContainer.class))).thenReturn("mockPayload");

        /* Stable time. */
        mockStatic(System.class);
        long now = 456L;
        when(System.currentTimeMillis()).thenReturn(now);

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
        ingestionHttp.setServerUrl("http://mock");
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestionHttp.sendAsync(appSecret, installId, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(DefaultHttpClient.APP_SECRET, appSecret);
        expectedHeaders.put(IngestionHttp.INSTALL_ID, installId.toString());
        verify(httpClient).callAsync(eq("http://mock/" + IngestionHttp.API_PATH), eq(METHOD_POST), eq(expectedHeaders), notNull(HttpClient.CallTemplate.class), eq(serviceCallback));
        assertNotNull(callTemplate.get());
        assertEquals("mockPayload", callTemplate.get().buildRequestBody());

        /* Verify toffset manipulation. */
        verify(log).setToffset(now - logAbsoluteTime);
        verify(log).setToffset(logAbsoluteTime);

        /* Verify close. */
        ingestionHttp.close();
        verify(httpClient).close();
    }

    @Test
    public void failedSerialization() throws Exception {

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log = mock(Log.class);
        long logAbsoluteTime = 123L;
        when(log.getToffset()).thenReturn(logAbsoluteTime);
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);
        LogSerializer serializer = mock(LogSerializer.class);
        JSONException exception = new JSONException("mock");
        when(serializer.serializeContainer(any(LogContainer.class))).thenThrow(exception);

        /* Stable time. */
        mockStatic(System.class);
        long now = 456L;
        when(System.currentTimeMillis()).thenReturn(now);

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
        ingestionHttp.setServerUrl("http://mock");
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestionHttp.sendAsync(appSecret, installId, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(DefaultHttpClient.APP_SECRET, appSecret);
        expectedHeaders.put(IngestionHttp.INSTALL_ID, installId.toString());
        verify(httpClient).callAsync(eq("http://mock/logs?api_version=1.0.0-preview20160914"), eq(METHOD_POST), eq(expectedHeaders), notNull(HttpClient.CallTemplate.class), eq(serviceCallback));
        assertNotNull(callTemplate.get());

        try {
            callTemplate.get().buildRequestBody();
            Assert.fail("Expected json exception");
        } catch (JSONException ignored) {
        }

        /* Verify toffset manipulation. */
        verify(log).setToffset(now - logAbsoluteTime);
        verify(log).setToffset(logAbsoluteTime);

        /* Verify close. */
        ingestionHttp.close();
        verify(httpClient).close();
    }
}