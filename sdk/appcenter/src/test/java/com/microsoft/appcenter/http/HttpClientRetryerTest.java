/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import android.os.Handler;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.http.DefaultHttpClient.CONTENT_TYPE_KEY;
import static com.microsoft.appcenter.http.DefaultHttpClient.CONTENT_TYPE_VALUE;
import static com.microsoft.appcenter.http.DefaultHttpClient.X_MS_RETRY_AFTER_MS_HEADER;
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

    private static void verifyDelayFromHeader(Handler handler, final long retryAfter) {
        verify(handler).postDelayed(any(Runnable.class), longThat(new ArgumentMatcher<Long>() {

            @Override
            public boolean matches(Object argument) {
                long interval = (Long) argument;
                return interval == retryAfter;
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
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded("mockSuccessPayload", null);
                return call;
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        HttpClientRetryer retryer = new HttpClientRetryer(httpClient);
        retryer.callAsync(null, null, null, null, callback);
        verify(callback).onCallSucceeded("mockSuccessPayload", null);
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
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded("mockSuccessPayload", null);
                return mock(ServiceCall.class);
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        HttpClient retryer = new HttpClientRetryer(httpClient, handler);
        simulateRetryAfterDelay(handler);
        retryer.callAsync(null, null, null, null, callback);
        verifyDelay(handler, 0);
        verifyNoMoreInteractions(handler);
        verify(callback).onCallSucceeded("mockSuccessPayload", null);
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
    public void delayUsingRetryHeader() {

        /* Mock httpException onCallFailed with the HTTP Code 429 (Too many Requests) and the x-ms-retry-after-ms header set. */
        long retryAfterMS = 1234;
        Map<String, String> responseHeader = new HashMap<>();
        responseHeader.put(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);
        responseHeader.put(X_MS_RETRY_AFTER_MS_HEADER, Long.toString(retryAfterMS));
        final HttpException expectedException = new HttpException(429, "call hit the retry limit", responseHeader);

        final ServiceCallback callback = mock(ServiceCallback.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallFailed(expectedException);
                return mock(ServiceCall.class);
            }
        }).doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded("mockSuccessPayload", null);
                return mock(ServiceCall.class);
            }
        }).when(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        HttpClient retryer = new HttpClientRetryer(httpClient, handler);
        simulateRetryAfterDelay(handler);

        /* Make the call. */
        retryer.callAsync(null, null, null, null, callback);

        /* Verify that onCallFailed we actually check for the response header and use that value to set the delay on the retry call. */
        verifyDelayFromHeader(handler, retryAfterMS);
        verifyNoMoreInteractions(handler);
        verify(callback).onCallSucceeded("mockSuccessPayload", null);
        verifyNoMoreInteractions(callback);
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
