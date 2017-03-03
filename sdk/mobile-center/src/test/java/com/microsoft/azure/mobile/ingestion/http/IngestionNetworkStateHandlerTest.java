package com.microsoft.azure.mobile.ingestion.http;

import com.microsoft.azure.mobile.ingestion.Ingestion;
import com.microsoft.azure.mobile.ingestion.ServiceCall;
import com.microsoft.azure.mobile.ingestion.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class IngestionNetworkStateHandlerTest {

    @Test
    public void success() throws IOException {

        /* Configure mock wrapped API. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        Ingestion ingestion = mock(Ingestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ServiceCallback serviceCallback = (ServiceCallback) invocationOnMock.getArguments()[3];
                serviceCallback.onCallSucceeded();
                serviceCallback.onCallSucceeded();
                return call;
            }
        }).when(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network is initially up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        Ingestion decorator = new IngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appSecret, installId, container, callback);
        verify(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback).onCallSucceeded();
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void failure() throws IOException {

        /* Configure mock wrapped API. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        Ingestion ingestion = mock(Ingestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ServiceCallback serviceCallback = (ServiceCallback) invocationOnMock.getArguments()[3];
                serviceCallback.onCallFailed(new HttpException(503));
                serviceCallback.onCallFailed(new SocketException());
                return call;
            }
        }).when(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network is initially up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        Ingestion decorator = new IngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appSecret, installId, container, callback);
        verify(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback).onCallFailed(new HttpException(503));
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void networkDownBecomesUp() throws IOException {

        /* Configure mock wrapped API. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        Ingestion ingestion = mock(Ingestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).onCallSucceeded();
                return call;
            }
        }).when(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false).thenReturn(true);

        /* Test call. */
        IngestionNetworkStateHandler decorator = new IngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appSecret, installId, container, callback);

        /* Network is down: no call to target API must be done. */
        verify(ingestion, times(0)).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback, times(0)).onCallSucceeded();

        /* Network now up: call must be done and succeed. */
        decorator.onNetworkStateUpdated(true);
        verify(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback).onCallSucceeded();

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void networkDownCancelBeforeUp() throws IOException {

        /* Configure mock wrapped API. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        Ingestion ingestion = mock(Ingestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).onCallSucceeded();
                return call;
            }
        }).when(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false).thenReturn(true);

        /* Test call and cancel right away. */
        IngestionNetworkStateHandler decorator = new IngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appSecret, installId, container, callback).cancel();

        /* Network now up, verify no interaction with anything. */
        decorator.onNetworkStateUpdated(true);
        verifyNoMoreInteractions(ingestion);
        verifyNoMoreInteractions(call);
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void cancelRunningCall() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        Ingestion ingestion = mock(Ingestion.class);
        final AtomicReference<Thread> threadRef = new AtomicReference<>();
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            sleep(200);
                            ((ServiceCallback) invocationOnMock.getArguments()[3]).onCallSucceeded();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
                threadRef.set(thread);
                return call;
            }
        }).when(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                threadRef.get().interrupt();
                return null;
            }
        }).when(call).cancel();

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        IngestionNetworkStateHandler decorator = new IngestionNetworkStateHandler(ingestion, networkStateHelper);
        ServiceCall decoratorCall = decorator.sendAsync(appSecret, installId, container, callback);

        /* Wait some time. */
        Thread.sleep(100);

        /* Cancel. */
        decoratorCall.cancel();

        /* Verify that the call was attempted then canceled. */
        verify(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        verify(call).cancel();
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void cancelRunningCallByClosing() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        Ingestion ingestion = mock(Ingestion.class);
        final AtomicReference<Thread> threadRef = new AtomicReference<>();
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            sleep(200);
                            ((ServiceCallback) invocationOnMock.getArguments()[3]).onCallSucceeded();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
                threadRef.set(thread);
                return call;
            }
        }).when(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                threadRef.get().interrupt();
                return null;
            }
        }).when(ingestion).close();

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        IngestionNetworkStateHandler decorator = new IngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appSecret, installId, container, callback);

        /* Wait some time. */
        Thread.sleep(100);

        /* Cancel by closing. */
        decorator.close();

        /* Verify that the call was attempted then canceled. */
        verify(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        verify(ingestion).close();
        verify(call).cancel();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void networkLossDuringCall() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        String appSecret = UUIDUtils.randomUUID().toString();
        UUID installId = UUIDUtils.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        Ingestion ingestion = mock(Ingestion.class);
        final AtomicReference<Thread> threadRef = new AtomicReference<>();
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            sleep(200);
                            ((ServiceCallback) invocationOnMock.getArguments()[3]).onCallSucceeded();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
                threadRef.set(thread);
                return call;
            }
        }).when(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                threadRef.get().interrupt();
                return null;
            }
        }).when(call).cancel();

        /* Simulate network up then down then up again. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true).thenReturn(false).thenReturn(true);

        /* Test call. */
        IngestionNetworkStateHandler decorator = new IngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appSecret, installId, container, callback);

        /* Wait some time. */
        Thread.sleep(100);

        /* Lose network. */
        decorator.onNetworkStateUpdated(false);

        /* Verify that the call was attempted then canceled. */
        verify(ingestion).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        verify(call).cancel();
        verifyNoMoreInteractions(callback);

        /* Then up again. */
        decorator.onNetworkStateUpdated(true);
        verify(ingestion, times(2)).sendAsync(eq(appSecret), eq(installId), eq(container), any(ServiceCallback.class));
        Thread.sleep(300);
        verify(callback).onCallSucceeded();
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void setLogUrl() {
        Ingestion ingestion = mock(Ingestion.class);
        Ingestion retryer = new IngestionNetworkStateHandler(ingestion, mock(NetworkStateHelper.class));
        String logUrl = "http://someLogUrl";
        retryer.setLogUrl(logUrl);
        verify(ingestion).setLogUrl(logUrl);
    }
}
