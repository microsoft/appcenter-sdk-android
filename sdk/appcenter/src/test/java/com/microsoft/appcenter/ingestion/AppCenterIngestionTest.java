/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
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
import static com.microsoft.appcenter.utils.PrefStorageConstants.ALLOWED_NETWORK_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@PrepareForTest({
        AppCenterIngestion.class,
        AppCenterLog.class,
        SharedPreferencesManager.class
})
public class AppCenterIngestionTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    private HttpClient mHttpClient;

    @Before
    public void setUp() {
        spy(AppCenterLog.class);
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(ALLOWED_NETWORK_REQUEST, true)).thenReturn(true);
    }

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
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        when(mHttpClient.callAsync(anyString(), anyString(), anyMap(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {
            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });

        /* Test calling code. */
        AppCenterIngestion ingestion = new AppCenterIngestion(mHttpClient, serializer);
        ingestion.setLogUrl("http://mock");
        String appSecret = UUID.randomUUID().toString();
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestion.sendAsync(appSecret, installId, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(Constants.APP_SECRET, appSecret);
        expectedHeaders.put(AppCenterIngestion.INSTALL_ID, installId.toString());
        verify(mHttpClient).callAsync(eq("http://mock" + AppCenterIngestion.API_PATH), eq(METHOD_POST), eq(expectedHeaders), notNull(), eq(serviceCallback));
        assertNotNull(callTemplate.get());
        assertEquals("mockPayload", callTemplate.get().buildRequestBody());

        /* Verify close. */
        ingestion.close();
        verify(mHttpClient).close();

        /* Verify reopen. */
        ingestion.reopen();
        verify(mHttpClient).reopen();
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
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        when(mHttpClient.callAsync(anyString(), anyString(), anyMap(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });

        /* Test calling code. */
        AppCenterIngestion ingestion = new AppCenterIngestion(mHttpClient, serializer);
        ingestion.setLogUrl("http://mock");
        String appSecret = UUID.randomUUID().toString();
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertEquals(call, ingestion.sendAsync(appSecret, installId, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(Constants.APP_SECRET, appSecret);
        expectedHeaders.put(AppCenterIngestion.INSTALL_ID, installId.toString());
        verify(mHttpClient).callAsync(eq("http://mock/logs?api-version=1.0.0"), eq(METHOD_POST), eq(expectedHeaders), notNull(), eq(serviceCallback));
        assertNotNull(callTemplate.get());

        try {
            callTemplate.get().buildRequestBody();
            Assert.fail("Expected json exception");
        } catch (JSONException ignored) {
        }

        /* Verify close. */
        ingestion.close();
        verify(mHttpClient).close();
    }

    @Test
    public void onBeforeCalling() throws Exception {

        /* Mock instances. */
        URL url = new URL("http://mock/path/file");
        String appSecret = UUID.randomUUID().toString();
        String obfuscatedSecret = HttpUtils.hideSecret(appSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put("Another-Header", "Another-Value");
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret);
        AppCenterLog.setLogLevel(android.util.Log.VERBOSE);
        mockStatic(AppCenterLog.class);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(url, headers);

        /* Verify url log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.verbose(anyString(), contains(url.toString()));

        /* Verify header log. */
        for (Map.Entry<String, String> header : headers.entrySet()) {
            verifyStatic(AppCenterLog.class);
            AppCenterLog.verbose(anyString(), contains(header.getValue()));
        }

        /* Put app secret to header. */
        headers.put(Constants.APP_SECRET, appSecret);
        callTemplate.onBeforeCalling(url, headers);

        /* Verify app secret is in log. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.verbose(anyString(), contains(obfuscatedSecret));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onBeforeCallingWithAnotherLogLevel() {

        /* Mock instances. */
        String appSecret = UUID.randomUUID().toString();
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret);

        /* Change log level. */
        AppCenterLog.setLogLevel(android.util.Log.WARN);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(mock(URL.class), mock(Map.class));

        /* Verify. */
        verifyStatic(AppCenterLog.class, never());
        AppCenterLog.verbose(anyString(), anyString());
    }

    @Test
    public void sendLogsWhenIngestionDisable() throws JSONException {
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getBoolean(ALLOWED_NETWORK_REQUEST, true)).thenReturn(false);

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log = mock(Log.class);
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);
        LogSerializer serializer = mock(LogSerializer.class);
        when(serializer.serializeContainer(any(LogContainer.class))).thenReturn("mockPayload");

        /* Configure mock HTTP. */
        final ServiceCall call = mock(ServiceCall.class);

        /* Test calling code. */
        AppCenterIngestion ingestion = new AppCenterIngestion(mHttpClient, serializer);
        ingestion.setLogUrl("http://mock");
        String appSecret = UUID.randomUUID().toString();
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertNull(ingestion.sendAsync(appSecret, installId, container, serviceCallback));

        /* Verify call to http client. */
        HashMap<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(Constants.APP_SECRET, appSecret);
        expectedHeaders.put(AppCenterIngestion.INSTALL_ID, installId.toString());
        verify(mHttpClient, never()).callAsync(eq("http://mock" + AppCenterIngestion.API_PATH), eq(METHOD_POST), eq(expectedHeaders), notNull(), eq(serviceCallback));
    }

    private HttpClient.CallTemplate getCallTemplate(String appSecret) {

        /* Configure mock HTTP to get an instance of IngestionCallTemplate. */
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        when(mHttpClient.callAsync(anyString(), anyString(), anyMap(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });
        AppCenterIngestion ingestion = new AppCenterIngestion(mHttpClient, mock(LogSerializer.class));
        ingestion.setLogUrl("http://mock");
        assertEquals(call, ingestion.sendAsync(appSecret, UUID.randomUUID(), mock(LogContainer.class), mock(ServiceCallback.class)));
        return callTemplate.get();
    }
}