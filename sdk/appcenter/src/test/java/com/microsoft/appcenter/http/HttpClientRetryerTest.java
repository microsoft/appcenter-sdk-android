package com.microsoft.appcenter.http;

import android.os.Handler;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.SocketException;
import java.net.UnknownHostException;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.longThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("unused")
public class HttpClientRetryerTest {

    private static void simulateRetryAfterDelay(Handler handler) {
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(handler).postDelayed(any(Runnable.class), anyLong());
    }

    private static void verifyDelay(Handler handler, final int retryIndex) {
        verify(handler).postDelayed(any(Runnable.class), longThat(new ArgumentMatcher<Long>() {

            @Override
            public boolean matches(Object argument) {
                long interval = (Long) argument;
                long retryInterval = HttpClientRetryer.RETRY_INTERVALS[retryIndex];
                return interval >= retryInterval / 2 && interval <= retryInterval;
            }
        }));
    }

    @Test
    public void success() {
        final ServiceCall call = mock(ServiceCall.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded("mockSuccessPayload");
                return call;
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        HttpClientRetryer retryer = new HttpClientRetryer(httpClient);
        retryer.callAsync(null, null, null, null, callback);
        verify(callback).onCallSucceeded("mockSuccessPayload");
        verifyNoMoreInteractions(callback);
        verifyNoMoreInteractions(call);
    }

    @Test
    public void successAfterOneRetry() {
        final ServiceCallback callback = mock(ServiceCallback.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallFailed(new SocketException());
                return mock(ServiceCall.class);
            }
        }).doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded("mockSuccessPayload");
                return mock(ServiceCall.class);
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        HttpClient retryer = new HttpClientRetryer(httpClient, handler);
        simulateRetryAfterDelay(handler);
        retryer.callAsync(null, null, null, null, callback);
        verifyDelay(handler, 0);
        verifyNoMoreInteractions(handler);
        verify(callback).onCallSucceeded("mockSuccessPayload");
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void retryOnceThenFail() {
        final HttpException expectedException = new HttpException(403);
        final ServiceCallback callback = mock(ServiceCallback.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallFailed(new UnknownHostException());
                return mock(ServiceCall.class);
            }
        }).doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallFailed(expectedException);
                return mock(ServiceCall.class);
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        HttpClient retryer = new HttpClientRetryer(httpClient, handler);
        simulateRetryAfterDelay(handler);
        retryer.callAsync(null, null, null, null, callback);
        verifyDelay(handler, 0);
        verifyNoMoreInteractions(handler);
        verify(callback).onCallFailed(any(Exception.class));
        verify(callback).onCallFailed(expectedException);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void exhaustRetries() {
        final ServiceCall call = mock(ServiceCall.class);
        ServiceCallback callback = mock(ServiceCallback.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallFailed(new HttpException(408));
                return call;
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        HttpClient retryer = new HttpClientRetryer(httpClient, handler);
        simulateRetryAfterDelay(handler);
        retryer.callAsync(null, null, null, null, callback);
        verifyDelay(handler, 0);
        verifyDelay(handler, 1);
        verifyDelay(handler, 2);
        verifyNoMoreInteractions(handler);
        verify(callback).onCallFailed(new HttpException(408));
        verifyNoMoreInteractions(callback);
        verifyNoMoreInteractions(call);
    }

    @Test
    public void cancel() throws InterruptedException {
        final ServiceCall call = mock(ServiceCall.class);
        ServiceCallback callback = mock(ServiceCallback.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallFailed(new HttpException(503));
                return call;
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        HttpClient retryer = new HttpClientRetryer(httpClient, handler);
        retryer.callAsync(null, null, null, null, callback).cancel();
        Thread.sleep(500);
        verifyNoMoreInteractions(callback);
        verify(call).cancel();
    }
}
