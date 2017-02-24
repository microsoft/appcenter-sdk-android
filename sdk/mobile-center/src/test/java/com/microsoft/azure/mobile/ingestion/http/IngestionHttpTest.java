package com.microsoft.azure.mobile.ingestion.http;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.ingestion.ServiceCall;
import com.microsoft.azure.mobile.ingestion.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest(IngestionHttp.class)
public class IngestionHttpTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    /**
     * Simulate ASyncTask. It's not in @Before because some tests like cancel must not use this.
     */
    private static void mockCall() throws Exception {

        /* Mock AsyncTask... */
        whenNew(IngestionHttp.Call.class).withAnyArguments().thenAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final IngestionHttp.Call call = new IngestionHttp.Call(invocation.getArguments()[0].toString(), (LogSerializer) invocation.getArguments()[1], (String) invocation.getArguments()[2], (UUID) invocation.getArguments()[3], (LogContainer) invocation.getArguments()[4], (ServiceCallback) invocation.getArguments()[5]);
                IngestionHttp.Call spyCall = spy(call);
                when(spyCall.executeOnExecutor(any(Executor.class))).then(new Answer<IngestionHttp.Call>() {

                    @Override
                    public IngestionHttp.Call answer(InvocationOnMock invocation) throws Throwable {
                        call.onPostExecute(call.doInBackground());
                        return call;
                    }
                });
                return spyCall;
            }
        });
    }

    @Test
    public void success() throws Exception {

        /* Set log level to verbose to test shorter app secret as well. */
        MobileCenter.setLogLevel(VERBOSE);

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log = mock(Log.class);
        long logAbsoluteTime = 123L;
        when(log.getToffset()).thenReturn(logAbsoluteTime);
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);

        /* Stable time. */
        mockStatic(System.class);
        long now = 456L;
        when(System.currentTimeMillis()).thenReturn(now);

        /* Configure mock HTTP. */
        URL url = mock(URL.class);
        whenNew(URL.class).withArguments("http://mock/logs?api_version=1.0.0-preview20160914").thenReturn(url);
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(200);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("OK".getBytes()));

        /* Configure API client. */
        LogSerializer serializer = mock(LogSerializer.class);
        when(serializer.serializeContainer(any(LogContainer.class))).thenReturn("mockPayload");
        IngestionHttp httpClient = new IngestionHttp(serializer);
        httpClient.setLogUrl("http://mock");

        /* Test calling code. Use shorter but valid app secret. */
        String appSecret = "SHORT";
        UUID installId = UUIDUtils.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.sendAsync(appSecret, installId, container, serviceCallback);
        verify(serviceCallback).onCallSucceeded();
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).setRequestProperty("Content-Type", "application/json");
        verify(urlConnection).setRequestProperty("App-Secret", appSecret);
        verify(urlConnection).setRequestProperty("Install-ID", installId.toString());
        verify(urlConnection).disconnect();
        httpClient.close();

        /* Verify payload and toffset manipulation. */
        String sentPayload = buffer.toString("UTF-8");
        assertEquals("mockPayload", sentPayload);
        verify(log).setToffset(now - logAbsoluteTime);
        verify(log).setToffset(logAbsoluteTime);
    }

    @Test
    public void error503() throws Exception {

        /* Set log level to verbose to test shorter app secret as well. */
        MobileCenter.setLogLevel(INFO);

        /* Build some payload. */
        LogContainer container = new LogContainer();
        Log log = mock(Log.class);
        long logAbsoluteTime = 123L;
        when(log.getToffset()).thenReturn(logAbsoluteTime);
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        container.setLogs(logs);

        /* Stable time. */
        mockStatic(System.class);
        long now = 456L;
        when(System.currentTimeMillis()).thenReturn(now);

        /* Configure mock HTTP. */
        URL url = mock(URL.class);
        whenNew(URL.class).withAnyArguments().thenReturn(url);
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getResponseCode()).thenReturn(503);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(buffer);
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Busy".getBytes()));

        /* Configure API client. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.serializeContainer(container)).thenReturn("");
        IngestionHttp httpClient = new IngestionHttp(logSerializer);

        /* Test calling code. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.sendAsync(appSecret, installId, container, serviceCallback);
        verify(serviceCallback).onCallFailed(new HttpException(503, "Busy"));
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).disconnect();
        verify(log).setToffset(now - logAbsoluteTime);
        verify(log).setToffset(logAbsoluteTime);
    }

    @Test
    public void cancel() throws Exception {

        /* Mock AsyncTask... */
        IngestionHttp.Call mockCall = mock(IngestionHttp.Call.class);
        whenNew(IngestionHttp.Call.class).withAnyArguments().thenReturn(mockCall);
        when(mockCall.isCancelled()).thenReturn(false).thenReturn(true);
        IngestionHttp httpClient = new IngestionHttp(mock(LogSerializer.class));
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        ServiceCall call = httpClient.sendAsync(UUIDUtils.randomUUID().toString(), UUIDUtils.randomUUID(), new LogContainer(), serviceCallback);

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
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        IngestionHttp httpClient = new IngestionHttp(mock(LogSerializer.class));
        mockCall();
        httpClient.sendAsync(UUIDUtils.randomUUID().toString(), UUIDUtils.randomUUID(), new LogContainer(), serviceCallback);
        verify(serviceCallback).onCallFailed(exception);
        verifyZeroInteractions(serviceCallback);
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

        /* Stable time. */
        mockStatic(System.class);
        long now = 456L;
        when(System.currentTimeMillis()).thenReturn(now);

        /* Configure mock HTTP. */
        URL url = mock(URL.class);
        whenNew(URL.class).withAnyArguments().thenReturn(url);
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(urlConnection);

        /* Configure API client. */
        LogSerializer serializer = mock(LogSerializer.class);
        JSONException exception = new JSONException("mock");
        when(serializer.serializeContainer(any(LogContainer.class))).thenThrow(exception);
        IngestionHttp httpClient = new IngestionHttp(serializer);

        /* Test calling code. */
        String appSecret = UUID.randomUUID().toString();
        UUID installId = UUID.randomUUID();
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        mockCall();
        httpClient.sendAsync(appSecret, installId, container, serviceCallback);
        verify(serviceCallback).onCallFailed(exception);
        verifyNoMoreInteractions(serviceCallback);
        verify(urlConnection).disconnect();
        verify(log).setToffset(now - logAbsoluteTime);
        verify(log).setToffset(logAbsoluteTime);
    }

    @Test
    @PrepareForTest(HandlerUtils.class)
    public void rejectedAsyncTask() throws Exception {

        /* Mock HandlerUtils to simulate call from background (this unit test) to main (mock) thread. */
        final Semaphore semaphore = new Semaphore(0);
        mockStatic(HandlerUtils.class);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
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
        IngestionHttp.Call call = mock(IngestionHttp.Call.class);
        whenNew(IngestionHttp.Call.class).withAnyArguments().thenReturn(call);
        RejectedExecutionException exception = new RejectedExecutionException();
        when(call.executeOnExecutor(any(Executor.class))).thenThrow(exception);
        IngestionHttp httpClient = new IngestionHttp(mock(LogSerializer.class));

        /* Test. */
        ServiceCallback serviceCallback = mock(ServiceCallback.class);
        assertNotNull(httpClient.sendAsync("", UUID.randomUUID(), mock(LogContainer.class), serviceCallback));

        /* Verify the callback call from "main" thread. */
        semaphore.acquireUninterruptibly();
        verify(serviceCallback).onCallFailed(exception);
        verify(serviceCallback, never()).onCallSucceeded();
    }
}