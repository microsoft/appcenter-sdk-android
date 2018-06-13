package com.microsoft.appcenter.ingestion;

import android.content.Context;

import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.appcenter.BuildConfig.VERSION_NAME;
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
@PrepareForTest({OneCollectorIngestion.class, AppCenterLog.class})
public class OneCollectorIngestionTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void sendAsync() throws Exception {

        /* Mock time */
        mockStatic(System.class);
        when(System.currentTimeMillis()).thenReturn(1234L);

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log1 = mock(Log.class);
        when(log1.getTransmissionTargetTokens()).thenReturn(Collections.singleton("token1"));
        Log log2 = mock(Log.class);
        when(log2.getTransmissionTargetTokens()).thenReturn(new HashSet<>(Arrays.asList("token2", "token3")));
        List<Log> logs = new ArrayList<>();
        logs.add(log1);
        logs.add(log2);
        container.setLogs(logs);
        LogSerializer serializer = mock(LogSerializer.class);
        when(serializer.serializeLog(log1)).thenReturn("mockPayload1");
        when(serializer.serializeLog(log2)).thenReturn("mockPayload2");

        /* Configure mock HTTP. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });

        /* Test calling code. */
        OneCollectorIngestion ingestion = new OneCollectorIngestion(mock(Context.class), serializer);
        ingestion.setLogUrl("http://mock");
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestion.sendAsync(null, null, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(OneCollectorIngestion.API_KEY, "token1,token2,token3");
        expectedHeaders.put(OneCollectorIngestion.CLIENT_VERSION_KEY, String.format("ACS-Android-Java-no-%s-no", VERSION_NAME));
        expectedHeaders.put(OneCollectorIngestion.UPLOAD_TIME_KEY, "1234");
        expectedHeaders.put(DefaultHttpClient.CONTENT_TYPE_KEY, "application/x-json-stream; charset=utf-8");
        verify(httpClient).callAsync(eq("http://mock"), eq(METHOD_POST), eq(expectedHeaders), notNull(HttpClient.CallTemplate.class), eq(serviceCallback));
        assertNotNull(callTemplate.get());
        assertEquals("mockPayload1\nmockPayload2\n", callTemplate.get().buildRequestBody());

        /* Verify close. */
        ingestion.close();
        verify(httpClient).close();

        /* Verify reopen. */
        ingestion.reopen();
        verify(httpClient).reopen();
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
        when(serializer.serializeLog(log)).thenThrow(exception);

        /* Configure mock HTTP. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });

        /* Test calling code. */
        OneCollectorIngestion ingestion = new OneCollectorIngestion(mock(Context.class), serializer);
        ingestion.setLogUrl("http://mock");
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestion.sendAsync(null, null, container, serviceCallback));

        /* Verify call to http client. */
        assertNotNull(callTemplate.get());
        try {
            callTemplate.get().buildRequestBody();
            Assert.fail("Expected json exception");
        } catch (JSONException ignored) {
        }

        /* Verify close. */
        ingestion.close();
        verify(httpClient).close();
    }

    @Test
    public void onBeforeCalling() throws Exception {

        /* Mock instances. */
        URL url = new URL("http://mock/path/file");
        String apiKeys = UUIDUtils.randomUUID().toString();
        String obfuscatedSecret = HttpUtils.hideSecret(apiKeys);
        Map<String, String> headers = new HashMap<>();
        headers.put("Another-Header", "Another-Value");
        HttpClient.CallTemplate callTemplate = getCallTemplate();
        AppCenterLog.setLogLevel(android.util.Log.VERBOSE);
        mockStatic(AppCenterLog.class);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(url, headers);

        /* Verify url log. */
        verifyStatic();
        AppCenterLog.verbose(anyString(), contains(url.toString()));

        /* Verify header log. */
        for (Map.Entry<String, String> header : headers.entrySet()) {
            verifyStatic();
            AppCenterLog.verbose(anyString(), contains(header.getValue()));
        }

        /* Put app secret to header. */
        headers.put(OneCollectorIngestion.API_KEY, apiKeys);
        callTemplate.onBeforeCalling(url, headers);

        /* Verify app secret is in log. */
        verifyStatic();
        AppCenterLog.verbose(anyString(), contains(obfuscatedSecret));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onBeforeCallingWithAnotherLogLevel() throws Exception {

        /* Mock instances. */
        String apiKey = UUIDUtils.randomUUID().toString();
        HttpClient.CallTemplate callTemplate = getCallTemplate();

        /* Change log level. */
        AppCenterLog.setLogLevel(android.util.Log.WARN);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(mock(URL.class), mock(Map.class));

        /* Verify. */
        verifyStatic(never());
        AppCenterLog.verbose(anyString(), anyString());
    }

    private HttpClient.CallTemplate getCallTemplate() throws Exception {

        /* Configure mock HTTP to get an instance of IngestionCallTemplate. */
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });
        OneCollectorIngestion ingestion = new OneCollectorIngestion(mock(Context.class), mock(LogSerializer.class));
        ingestion.setLogUrl("http://mock");
        assertEquals(call, ingestion.sendAsync(null, null, mock(LogContainer.class), mock(ServiceCallback.class)));
        return callTemplate.get();
    }
}