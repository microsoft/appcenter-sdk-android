package com.microsoft.appcenter.http;

import android.net.TrafficStats;
import android.os.Build;
import android.util.Log;

import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({DefaultHttpClient.class, TrafficStats.class, AppCenterLog.class})
public class DefaultHttpClientTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }

    /**
     * Simulate ASyncTask. It's not in @Before because some tests like cancel must not use this.
     */
    private static void mockCall() throws Exception {

        /* Mock AsyncTask... */
        whenNew(DefaultHttpClient.Call.class).withAnyArguments().thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {

                @SuppressWarnings("unchecked") final DefaultHttpClient.Call call = new DefaultHttpClient.Call(invocation.getArguments()[0].toString(), invocation.getArguments()[1].toString(), (Map<String, String>) invocation.getArguments()[2], (HttpClient.CallTemplate) invocation.getArguments()[3], (ServiceCallback) invocation.getArguments()[4]);
                DefaultHttpClient.Call spyCall = spy(call);
                when(spyCall.executeOnExecutor(any(Executor.class))).then(new Answer<DefaultHttpClient.Call>() {

                    @Override
                    public DefaultHttpClient.Call answer(InvocationOnMock invocation) {
                        call.onPostExecute(call.doInBackground());
                        return call;
                    }
                });
                return spyCall;
            }
        });
        mockStatic(TrafficStats.class);
    }

    @Test
    public void tls1_2Enforcement() throws Exception {

        /* Configure mock HTTP. */
        mockCall();
        testTls1_2Setting(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1, 0);
        for (int apiLevel = Build.VERSION_CODES.JELLY_BEAN; apiLevel < Build.VERSION_CODES.KITKAT_WATCH; apiLevel++) {
            testTls1_2Setting(apiLevel, 1);
        }
        for (int apiLevel = Build.VERSION_CODES.KITKAT_WATCH; apiLevel <= Build.VERSION_CODES.O_MR1; apiLevel++) {
            testTls1_2Setting(apiLevel, 0);
        }
    }

    private void testTls1_2Setting(int apiLevel, int tlsSetExpectedCalls) throws Exception {
        String urlString = "http://mock/logs?api-version=1.0.0";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", apiLevel);
        httpClient.callAsync(urlString, METHOD_POST, new HashMap<String, String>(), null, mock(ServiceCallback.class));
        verify(urlConnection, times(tlsSetExpectedCalls)).setSSLSocketFactory(argThat(new ArgumentMatcher<SSLSocketFactory>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof TLS1_2SocketFactory;
            }
        }));
    }

    @Test
    public void post200() throws Exception {

        /* Mock related to pretty json logging. */
        mockStatic(AppCenterLog.class);
        when(AppCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);
        JSONObject jsonObject = mock(JSONObject.class);
        whenNew(JSONObject.class).withAnyArguments().thenReturn(jsonObject);
        String prettyString = "{\n" +
                "  \"a\": 1,\n" +
                "  \"b\": 2\n" +
                "}";
        when(jsonObject.toString(2)).thenReturn(prettyString);

        /* Configure mock HTTP. */
        String urlString = "http://mock/logs?api-version=1.0.0";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        when(callTemplate.buildRequestBody()).thenReturn("{a:1,b:2}");
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. Use shorter but valid app secret. */
        String appSecret = "SHORT";
        UUID installId = UUIDUtils.randomUUID();
        Map<String, String> headers = new HashMap<>();
        headers.put("App-Secret", appSecret);
        headers.put("Install-ID", installId.toString());
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_POST, headers, callTemplate, serviceCallback);
        verify(serviceCallback).onCallSucceeded("OK");
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestProperty("Content-Type", "application/json");
        verify(urlConnection, never()).setRequestProperty(eq("Content-Encoding"), anyString());
        verify(urlConnection).setRequestProperty("App-Secret", appSecret);
        verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        verify(urlConnection).setRequestMethod("POST");
        verify(urlConnection).setDoOutput(true);
        verify(urlConnection).disconnect();
        verify(callTemplate).onBeforeCalling(eq(url), anyMapOf(String.class, String.class));
        verify(callTemplate).buildRequestBody();
        httpClient.close();

        /* Verify payload. */
        String sentPayload = buffer.toString("UTF-8");
        assertEquals("{a:1,b:2}", sentPayload);

        /* Verify socket tagged to avoid strict mode error. */
        verifyStatic();
        TrafficStats.setThreadStatsTag(anyInt());
        verifyStatic();
        TrafficStats.clearThreadStatsTag();

        /* We enabled verbose and it's json, check pretty print. */
        verifyStatic();
        AppCenterLog.verbose(AppCenterLog.LOG_TAG, prettyString);
    }

    @Test
    public void post200WithoutCallTemplate() throws Exception {

        /* Configure mock HTTP. */
        String urlString = "http://mock/logs?api-version=1.0.0";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Configure API client. */
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. Use shorter but valid app secret. */
        String appSecret = "SHORT";
        UUID installId = UUIDUtils.randomUUID();
        Map<String, String> headers = new HashMap<>();
        headers.put("App-Secret", appSecret);
        headers.put("Install-ID", installId.toString());
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_POST, headers, null, serviceCallback);
        verify(serviceCallback).onCallSucceeded("OK");
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection, never()).setRequestProperty(eq("Content-Type"), anyString());
        verify(urlConnection, never()).setRequestProperty(eq("Content-Encoding"), anyString());
        verify(urlConnection).setRequestProperty("App-Secret", appSecret);
        verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        verify(urlConnection).setRequestMethod("POST");
        verify(urlConnection, never()).setDoOutput(true);
        verify(urlConnection).disconnect();
        httpClient.close();

        /* Verify payload. */
        String sentPayload = buffer.toString("UTF-8");
        assertEquals("", sentPayload);
    }

    @Test
    public void get200() throws Exception {

        /* Configure mock HTTP. */
        String urlString = "http://mock/get";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        ByteArrayInputStream inputStream = spy(new ByteArrayInputStream("OK".getBytes()));
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        Map<String, String> headers = new HashMap<>();
        headers.put("App-Secret", appSecret);
        headers.put("Install-ID", installId.toString());
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_GET, headers, callTemplate, serviceCallback);
        verify(serviceCallback).onCallSucceeded("OK");
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection, never()).setRequestProperty(eq("Content-Type"), anyString());
        verify(urlConnection, never()).setRequestProperty(eq("Content-Encoding"), anyString());
        verify(urlConnection).setRequestProperty("App-Secret", appSecret);
        verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        verify(urlConnection).setRequestMethod("GET");
        verify(urlConnection, never()).setDoOutput(true);
        verify(urlConnection).disconnect();
        verify(inputStream).close();
        verify(callTemplate).onBeforeCalling(eq(url), anyMapOf(String.class, String.class));
        verify(callTemplate, never()).buildRequestBody();
        httpClient.close();
    }

    @Test
    public void get2xx() throws Exception {

        /* Configure mock HTTP. */
        String urlString = "http://mock/get";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream("OK".getBytes());
        when(urlConnection.getInputStream()).thenReturn(inputStream);

        /* Configure API client. */
        DefaultHttpClient httpClient = new DefaultHttpClient();
        Map<String, String> headers = new HashMap<>();
        mockCall();

        for (int statusCode = 200; statusCode < 300; statusCode++) {

            /* Configure status code. */
            when(urlConnection.getResponseCode()).thenReturn(statusCode);

            /* Test calling code. */
            ServiceCallback serviceCallback = mock(ServiceCallback.class);
            httpClient.callAsync(urlString, METHOD_GET, headers, null, serviceCallback);
            verify(serviceCallback).onCallSucceeded("OK");
            verifyNoMoreInteractions(serviceCallback);

            /* Reset response stream. */
            inputStream.reset();
        }
        httpClient.close();
    }

    @Test
    public void get100() throws Exception {

        /* Configure mock HTTP. */
        URL url = mock(URL.class);
        whenNew(URL.class).withAnyArguments().thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(100);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Continue".getBytes()));

        /* Configure API client. */
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        Map<String, String> headers = new HashMap<>();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync("", METHOD_POST, headers, null, serviceCallback);
        verify(serviceCallback).onCallFailed(new HttpException(100, "Continue"));
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).disconnect();
    }

    @Test
    public void get200WithoutCallTemplate() throws Exception {

        /* Configure mock HTTP. */
        String urlString = "http://mock/get";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Configure API client. */
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        Map<String, String> headers = new HashMap<>();
        headers.put("App-Secret", appSecret);
        headers.put("Install-ID", installId.toString());
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_GET, headers, null, serviceCallback);
        verify(serviceCallback).onCallSucceeded("OK");
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection, never()).setRequestProperty(eq("Content-Type"), anyString());
        verify(urlConnection, never()).setRequestProperty(eq("Content-Encoding"), anyString());
        verify(urlConnection).setRequestProperty("App-Secret", appSecret);
        verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        verify(urlConnection).setRequestMethod("GET");
        verify(urlConnection, never()).setDoOutput(true);
        verify(urlConnection).disconnect();
        httpClient.close();
    }

    private void testPayloadLogging(final String payload, String mimeType) throws Exception {

        /* Set log level to verbose to test shorter app secret as well. */
        mockStatic(AppCenterLog.class);
        when(AppCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);

        /* Configure mock HTTP. */
        String urlString = "http://mock/get";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        ByteArrayInputStream inputStream = spy(new ByteArrayInputStream(payload.getBytes()));
        when(urlConnection.getInputStream()).thenReturn(inputStream);
        when(urlConnection.getHeaderField("Content-Type")).thenReturn(mimeType);

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        Map<String, String> headers = new HashMap<>();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_GET, headers, callTemplate, serviceCallback);
        verify(serviceCallback).onCallSucceeded(payload);
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestMethod("GET");
        verify(urlConnection, never()).setDoOutput(true);
        verify(urlConnection).disconnect();
        verify(inputStream).close();
        verify(callTemplate).onBeforeCalling(eq(url), anyMapOf(String.class, String.class));
        verify(callTemplate, never()).buildRequestBody();
        httpClient.close();

        /* Test binary placeholder used in logging code instead of real payload. */
        verifyStatic();
        AppCenterLog.verbose(anyString(), argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().contains(payload);
            }
        }));
    }

    @Test
    public void get200text() throws Exception {
        testPayloadLogging("Some text", "text/plain");
    }

    @Test
    public void get200json() throws Exception {
        testPayloadLogging("{}", "application/json");
    }

    @Test
    public void get200image() throws Exception {

        /* Mock verbose logs. */
        mockStatic(AppCenterLog.class);
        when(AppCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);

        /* Configure mock HTTP. */
        String urlString = "http://mock/get";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        ByteArrayInputStream inputStream = spy(new ByteArrayInputStream("fake binary".getBytes()));
        when(urlConnection.getInputStream()).thenReturn(inputStream);
        when(urlConnection.getHeaderField("Content-Type")).thenReturn("image/png");

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        Map<String, String> headers = new HashMap<>();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_GET, headers, callTemplate, serviceCallback);
        verify(serviceCallback).onCallSucceeded("fake binary");
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestMethod("GET");
        verify(urlConnection, never()).setDoOutput(true);
        verify(urlConnection).disconnect();
        verify(inputStream).close();
        verify(callTemplate).onBeforeCalling(eq(url), anyMapOf(String.class, String.class));
        verify(callTemplate, never()).buildRequestBody();
        httpClient.close();

        /* Test binary placeholder used in logging code instead of real payload. */
        verifyStatic();
        AppCenterLog.verbose(anyString(), argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                String logMessage = argument.toString();
                return logMessage.contains("<binary>") && !logMessage.contains("fake binary");
            }
        }));
    }

    @Test
    public void error503() throws Exception {

        /* Configure mock HTTP. */
        URL url = mock(URL.class);
        whenNew(URL.class).withAnyArguments().thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(503);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Busy".getBytes()));

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        when(callTemplate.buildRequestBody()).thenReturn("mockPayload");
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        Map<String, String> headers = new HashMap<>();
        headers.put("App-Secret", appSecret);
        headers.put("Install-ID", installId.toString());
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync("", METHOD_POST, headers, callTemplate, serviceCallback);
        verify(serviceCallback).onCallFailed(new HttpException(503, "Busy"));
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).disconnect();

        /* Verify socket tagged to avoid strict mode error. */
        verifyStatic();
        TrafficStats.setThreadStatsTag(anyInt());
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
    }

    @Test
    public void cancel() throws Exception {

        /* Mock AsyncTask... */
        DefaultHttpClient.Call mockCall = mock(DefaultHttpClient.Call.class);
        whenNew(DefaultHttpClient.Call.class).withAnyArguments().thenReturn(mockCall);
        when(mockCall.isCancelled()).thenReturn(false).thenReturn(true);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        ServiceCall call = httpClient.callAsync("", "", new HashMap<String, String>(), mock(HttpClient.CallTemplate.class), serviceCallback);

        /* Cancel and verify. */
        call.cancel();
        verify(mockCall).cancel(true);

        /* Calling cancel a second time should be allowed and ignored. */
        call.cancel();
        verify(mockCall, times(1)).cancel(true);
        verify(mockCall, never()).cancel(false);
    }

    @Test
    public void failedConnection() throws Exception {
        URL url = mock(URL.class);
        whenNew(URL.class).withAnyArguments().thenReturn(url);
        IOException exception = new IOException("mock");
        when(url.openConnection()).thenThrow(exception);
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        mockCall();
        httpClient.callAsync("", "", new HashMap<String, String>(), callTemplate, serviceCallback);
        verify(serviceCallback).onCallFailed(exception);
        verifyZeroInteractions(callTemplate);
        verifyZeroInteractions(serviceCallback);
    }

    @Test
    public void failedToReadResponse() throws Exception {
        URL url = mock(URL.class);
        whenNew(URL.class).withAnyArguments().thenReturn(url);
        IOException exception = new IOException("mock");
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        InputStream inputStream = mock(InputStream.class);
        when(urlConnection.getInputStream()).thenReturn(inputStream);
        InputStreamReader inputStreamReader = mock(InputStreamReader.class);
        whenNew(InputStreamReader.class).withAnyArguments().thenReturn(inputStreamReader);
        when(inputStreamReader.read(any(char[].class))).thenThrow(exception);
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        mockCall();
        httpClient.callAsync("", "", new HashMap<String, String>(), callTemplate, serviceCallback);
        verify(serviceCallback).onCallFailed(exception);
        verifyZeroInteractions(serviceCallback);
        verify(inputStream).close();
        verifyStatic();
        TrafficStats.setThreadStatsTag(anyInt());
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
    }

    @Test
    public void failedSerialization() throws Exception {

        /* Configure mock HTTP. */
        URL url = mock(URL.class);
        whenNew(URL.class).withAnyArguments().thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        JSONException exception = new JSONException("mock");
        when(callTemplate.buildRequestBody()).thenThrow(exception);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync("", METHOD_POST, new HashMap<String, String>(), callTemplate, serviceCallback);
        verify(serviceCallback).onCallFailed(exception);
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).disconnect();
        verifyStatic();
        TrafficStats.setThreadStatsTag(anyInt());
        verifyStatic();
        TrafficStats.clearThreadStatsTag();
    }

    @Test
    @PrepareForTest(HandlerUtils.class)
    public void rejectedAsyncTask() throws Exception {

        /* Mock HandlerUtils to simulate call from background (this unit test) to main (mock) thread. */
        final Semaphore semaphore = new Semaphore(0);
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                new Thread("rejectedAsyncTask.handler") {

                    @Override
                    public void run() {
                        ((Runnable) invocation.getArguments()[0]).run();
                        semaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Mock ingestion to fail on saturated executor in AsyncTask. */
        DefaultHttpClient.Call call = mock(DefaultHttpClient.Call.class);
        whenNew(DefaultHttpClient.Call.class).withAnyArguments().thenReturn(call);
        RejectedExecutionException exception = new RejectedExecutionException();
        when(call.executeOnExecutor(any(Executor.class))).thenThrow(exception);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test. */
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertNotNull(httpClient.callAsync("", "", new HashMap<String, String>(), mock(HttpClient.CallTemplate.class), serviceCallback));

        /* Verify the callback call from "main" thread. */
        semaphore.acquireUninterruptibly();
        verify(serviceCallback).onCallFailed(exception);
        verify(serviceCallback, never()).onCallSucceeded(notNull(String.class));
    }

    @Test
    public void sendGzipWithoutVerboseLogging() throws Exception {

        /* Mock no verbose logging. */
        mockStatic(AppCenterLog.class);
        when(AppCenterLog.getLogLevel()).thenReturn(Log.DEBUG);

        /* Configure mock HTTP. */
        String urlString = "http://mock";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Long mock payload. */
        StringBuilder payloadBuilder = new StringBuilder();
        for (int i = 0; i < 1400; i++) {
            payloadBuilder.append('a');
        }
        final String payload = payloadBuilder.toString();

        /* Compress payload for verification. */
        ByteArrayOutputStream gzipBuffer = new ByteArrayOutputStream(payload.length());
        GZIPOutputStream gzipStream = new GZIPOutputStream(gzipBuffer);
        gzipStream.write(payload.getBytes("UTF-8"));
        gzipStream.close();
        byte[] compressedBytes = gzipBuffer.toByteArray();

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        when(callTemplate.buildRequestBody()).thenReturn(payload);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "custom");
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_POST, headers, callTemplate, serviceCallback);
        verify(serviceCallback).onCallSucceeded("OK");
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestProperty("Content-Type", "custom");

        /* Also verify content type was set only once, json not applied. */
        verify(urlConnection).setRequestProperty(eq("Content-Type"), anyString());
        verify(urlConnection).setRequestProperty("Content-Encoding", "gzip");
        verify(urlConnection).setRequestMethod("POST");
        verify(urlConnection).setDoOutput(true);
        verify(urlConnection).disconnect();
        verify(callTemplate).onBeforeCalling(eq(url), anyMapOf(String.class, String.class));
        verify(callTemplate).buildRequestBody();
        httpClient.close();

        /* Verify payload compressed. */
        assertArrayEquals(compressedBytes, buffer.toByteArray());

        /* Check no payload logging since log level not enabled. */
        verifyStatic(never());
        AppCenterLog.verbose(anyString(), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                return argument.toString().contains(payload);
            }
        }));
    }

    @Test
    public void sendNoGzipWithPlainTextVerboseLogging() throws Exception {

        /* Mock verbose logging. */
        mockStatic(AppCenterLog.class);
        when(AppCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);

        /* Configure mock HTTP. */
        String urlString = "http://mock";
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments(urlString).thenReturn(url);
        HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Long mock payload. */
        StringBuilder payloadBuilder = new StringBuilder();
        for (int i = 0; i < 1399; i++) {
            payloadBuilder.append('a');
        }
        final String payload = payloadBuilder.toString();

        /* Configure API client. */
        HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        when(callTemplate.buildRequestBody()).thenReturn(payload);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        /* Test calling code. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "custom");
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.callAsync(urlString, METHOD_POST, headers, callTemplate, serviceCallback);
        verify(serviceCallback).onCallSucceeded("OK");
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestProperty("Content-Type", "custom");

        /* Also verify content type was set only once, json not applied. */
        verify(urlConnection).setRequestProperty(eq("Content-Type"), anyString());
        verify(urlConnection, never()).setRequestProperty("Content-Encoding", "gzip");
        verify(urlConnection).setRequestMethod("POST");
        verify(urlConnection).setDoOutput(true);
        verify(urlConnection).disconnect();
        verify(callTemplate).onBeforeCalling(eq(url), anyMapOf(String.class, String.class));
        verify(callTemplate).buildRequestBody();
        httpClient.close();

        /* Verify payload not compressed. */
        assertEquals(payload, buffer.toString());

        /* Check payload logged but not as JSON since different content type. */
        verifyStatic();
        AppCenterLog.verbose(anyString(), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                return argument.toString().contains(payload);
            }
        }));
    }
}