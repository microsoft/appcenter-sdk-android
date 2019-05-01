/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;

import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class HttpClientNetworkStateHandlerTest {

    @Test
    public void success() throws IOException {

        /* Configure mock wrapped API. */
        String url = "http://mock/call";
        Map<String, String> headers = new HashMap<>();
        final HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ServiceCallback serviceCallback = (ServiceCallback) invocationOnMock.getArguments()[4];
                serviceCallback.onCallSucceeded("mockPayload", null);
                return call;
            }
        }).when(httpClient).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));

        /* Simulate network is initially up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        HttpClient decorator = new HttpClientNetworkStateHandler(httpClient, networkStateHelper);
        verify(networkStateHelper).addListener(any(NetworkStateHelper.Listener.class));
        decorator.callAsync(url, METHOD_GET, headers, callTemplate, callback);
        verify(httpClient).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));
        verify(callback).onCallSucceeded("mockPayload", null);
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(httpClient).close();
        verify(networkStateHelper).removeListener(any(NetworkStateHelper.Listener.class));

        /* Reopen. */
        decorator.reopen();
        verify(httpClient).reopen();
        verify(networkStateHelper, times(2)).addListener(any(NetworkStateHelper.Listener.class));
    }

    @Test
    public void failure() throws IOException {

        /* Configure mock wrapped API. */
        String url = "http://mock/call";
        Map<String, String> headers = new HashMap<>();
        final HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ServiceCallback serviceCallback = (ServiceCallback) invocationOnMock.getArguments()[4];
                serviceCallback.onCallFailed(new HttpException(503));
                return call;
            }
        }).when(httpClient).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));

        /* Simulate network is initially up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Simulate state updated events first. It should not affect behavior. */
        HttpClientNetworkStateHandler decorator = new HttpClientNetworkStateHandler(httpClient, networkStateHelper);
        decorator.onNetworkStateUpdated(false);
        decorator.onNetworkStateUpdated(true);

        /* Test call. */
        decorator.callAsync(url, METHOD_GET, headers, callTemplate, callback);
        verify(httpClient).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));
        verify(callback).onCallFailed(new HttpException(503));
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(httpClient).close();
    }

    @Test
    public void networkDownBecomesUp() throws IOException {

        /* Configure mock wrapped API. */
        String url = "http://mock/call";
        Map<String, String> headers = new HashMap<>();
        final HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        HttpClient httpClient = mock(HttpClient.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded("", null);
                return call;
            }
        }).when(httpClient).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false).thenReturn(true);

        /* Test call. */
        HttpClientNetworkStateHandler decorator = new HttpClientNetworkStateHandler(httpClient, networkStateHelper);
        decorator.callAsync(url, METHOD_GET, headers, callTemplate, callback);

        /* Network is down: no call to target API must be done. */
        verify(httpClient, times(0)).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));
        verify(callback, times(0)).onCallSucceeded("", null);

        /* Network now up: call must be done and succeed. */
        decorator.onNetworkStateUpdated(true);
        verify(httpClient).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));
        verify(callback).onCallSucceeded("", null);

        /* Close. */
        decorator.close();
        verify(httpClient).close();
    }

    @Test
    public void networkDownCancelBeforeUp() throws IOException {

        /* Configure mock wrapped API. */
        String url = "http://mock/call";
        Map<String, String> headers = new HashMap<>();
        final HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded("", null);
                return call;
            }
        });

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false).thenReturn(true);

        /* Test call and cancel right away. */
        HttpClientNetworkStateHandler decorator = new HttpClientNetworkStateHandler(httpClient, networkStateHelper);
        decorator.callAsync(url, METHOD_GET, headers, callTemplate, callback).cancel();

        /* Network now up, verify no interaction with anything. */
        decorator.onNetworkStateUpdated(true);
        verifyNoMoreInteractions(httpClient);
        verifyNoMoreInteractions(call);
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(httpClient).close();
    }

    @Test
    public void cancelRunningCall() throws IOException {

        /* Configure mock wrapped API. */
        String url = "http://mock/call";
        Map<String, String> headers = new HashMap<>();
        final HttpClient.CallTemplate callTemplate = mock(HttpClient.CallTemplate.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class)))
                .thenReturn(call);

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        HttpClientNetworkStateHandler decorator = new HttpClientNetworkStateHandler(httpClient, networkStateHelper);
        ServiceCall decoratorCall = decorator.callAsync(url, METHOD_GET, headers, callTemplate, callback);

        /* Cancel. */
        decoratorCall.cancel();

        /* Verify that the call was attempted then canceled. */
        verify(httpClient).callAsync(eq(url), eq(METHOD_GET), eq(headers), eq(callTemplate), any(ServiceCallback.class));
        verify(call).cancel();
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(httpClient).close();
    }

    @Test(timeout=3000)
    public void changeNetworkConnectionDuringCallWithoutDeadlock() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.LOLLIPOP);

        /* Create mocks. */
        Context context = mock(Context.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        HttpClient httpClient = mock(HttpClient.class);

        /* Create test objects. */
        final CountDownLatch latch = new CountDownLatch(1);
        final NetworkStateHelper networkStateHelper = spy(new NetworkStateHelper(context));
        when(networkStateHelper.isNetworkConnected()).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                latch.await();
                return (Boolean)invocation.callRealMethod();
            }
        });
        networkStateHelper.addListener(new NetworkStateHelper.Listener() {

            @Override
            public void onNetworkStateUpdated(boolean connected) {
                latch.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        });
        final HttpClientNetworkStateHandler decorator = new HttpClientNetworkStateHandler(httpClient, networkStateHelper);

        /* Run some operation with another thread. */
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                String url = "http://mock/call";
                decorator.callAsync(url, METHOD_GET, null, null, null);
            }
        });
        thread.start();
        Thread.sleep(100);

        /* Simulate network lost event. */
        ArgumentCaptor<ConnectivityManager.NetworkCallback> callback = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(connectivityManager).registerNetworkCallback(any(NetworkRequest.class), callback.capture());
        callback.getValue().onAvailable(mock(Network.class));

        /* Clear the state. */
        thread.interrupt();
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }
}
