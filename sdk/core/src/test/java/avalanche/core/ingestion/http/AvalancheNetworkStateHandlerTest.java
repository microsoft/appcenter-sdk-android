package avalanche.core.ingestion.http;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCall;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.utils.NetworkStateHelper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AvalancheNetworkStateHandlerTest {

    @Test
    public void success() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ServiceCallback serviceCallback = (ServiceCallback) invocationOnMock.getArguments()[3];
                serviceCallback.success();
                serviceCallback.success();
                return call;
            }
        }).when(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network is initially up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        AvalancheIngestion decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback);
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback).success();
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void failure() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ServiceCallback serviceCallback = (ServiceCallback) invocationOnMock.getArguments()[3];
                serviceCallback.failure(new HttpException(503));
                serviceCallback.failure(new SocketException());
                return call;
            }
        }).when(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network is initially up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);

        /* Test call. */
        AvalancheIngestion decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback);
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback).failure(new HttpException(503));
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void networkDownBecomesUp() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).success();
                return call;
            }
        }).when(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false).thenReturn(true);

        /* Test call. */
        AvalancheIngestionNetworkStateHandler decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback);

        /* Network is down: no call to target API must be done. */
        verify(ingestion, times(0)).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback, times(0)).success();

        /* Network now up: call must be done and succeed. */
        decorator.onNetworkStateUpdated(true);
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback).success();

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void networkDownCancelBeforeUp() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).success();
                return call;
            }
        }).when(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));

        /* Simulate network down then becomes up. */
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false).thenReturn(true);

        /* Test call and cancel right away. */
        AvalancheIngestionNetworkStateHandler decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback).cancel();

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
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        final AtomicReference<Thread> threadRef = new AtomicReference<>();
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            sleep(200);
                            ((ServiceCallback) invocationOnMock.getArguments()[3]).success();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
                threadRef.set(thread);
                return call;
            }
        }).when(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
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
        AvalancheIngestionNetworkStateHandler decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        ServiceCall decoratorCall = decorator.sendAsync(appKey, installId, container, callback);

        /* Wait some time. */
        Thread.sleep(100);

        /* Cancel. */
        decoratorCall.cancel();

        /* Verify that the call was attempted then canceled. */
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(call).cancel();
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }

    @Test
    public void cancelRunningCallByClosing() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        final AtomicReference<Thread> threadRef = new AtomicReference<>();
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            sleep(200);
                            ((ServiceCallback) invocationOnMock.getArguments()[3]).success();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
                threadRef.set(thread);
                return call;
            }
        }).when(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
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
        AvalancheIngestionNetworkStateHandler decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback);

        /* Wait some time. */
        Thread.sleep(100);

        /* Cancel by closing. */
        decorator.close();

        /* Verify that the call was attempted then canceled. */
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(ingestion).close();
        verify(call).cancel();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void networkLossDuringCall() throws InterruptedException, IOException {

        /* Configure mock wrapped API. */
        UUID appKey = UUID.randomUUID();
        UUID installId = UUID.randomUUID();
        LogContainer container = mock(LogContainer.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        final ServiceCall call = mock(ServiceCall.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        final AtomicReference<Thread> threadRef = new AtomicReference<>();
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            sleep(200);
                            ((ServiceCallback) invocationOnMock.getArguments()[3]).success();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
                threadRef.set(thread);
                return call;
            }
        }).when(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
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
        AvalancheIngestionNetworkStateHandler decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback);

        /* Wait some time. */
        Thread.sleep(100);

        /* Lose network. */
        decorator.onNetworkStateUpdated(false);

        /* Verify that the call was attempted then canceled. */
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(call).cancel();
        verifyNoMoreInteractions(callback);

        /* Then up again. */
        decorator.onNetworkStateUpdated(true);
        verify(ingestion, times(2)).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        Thread.sleep(300);
        verify(callback).success();
        verifyNoMoreInteractions(callback);

        /* Close. */
        decorator.close();
        verify(ingestion).close();
    }
}
