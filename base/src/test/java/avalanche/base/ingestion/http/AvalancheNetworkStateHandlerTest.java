package avalanche.base.ingestion.http;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.UUID;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.utils.NetworkStateHelper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AvalancheNetworkStateHandlerTest {

    @Test
    public void success() throws InterruptedException, IOException {
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
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);
        AvalancheIngestion decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback);
        decorator.close();
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(ingestion).close();
        verify(callback).success();
        verifyNoMoreInteractions(callback);
        verifyNoMoreInteractions(call);
    }

    @Test
    public void networkDownBecomesUp() throws InterruptedException, IOException {
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
        NetworkStateHelper networkStateHelper = mock(NetworkStateHelper.class);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false).thenReturn(true);
        AvalancheIngestionNetworkStateHandler decorator = new AvalancheIngestionNetworkStateHandler(ingestion, networkStateHelper);
        decorator.sendAsync(appKey, installId, container, callback);
        decorator.onNetworkStateUpdated(true);
        verify(ingestion).sendAsync(eq(appKey), eq(installId), eq(container), any(ServiceCallback.class));
        verify(callback).success();
        decorator.close();
        verify(ingestion).close();
        verifyNoMoreInteractions(callback);
        verifyNoMoreInteractions(call);
    }
}
