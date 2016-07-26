package avalanche.core.ingestion.http;

import android.os.Handler;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCall;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.models.LogContainer;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.longThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("unused")
public class AvalancheIngestionRetryerTest {

    private static void simulateRetryAfterDelay(Handler handler) {
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
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
                long retryInterval = AvalancheIngestionRetryer.RETRY_INTERVALS[retryIndex];
                return interval >= retryInterval / 2 && interval <= retryInterval;
            }
        }));
    }

    @Test
    public void success() {
        final ServiceCall call = mock(ServiceCall.class);
        final ServiceCallback callback = mock(ServiceCallback.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).success();
                return call;
            }
        }).when(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion);
        retryer.sendAsync(null, null, null, callback);
        verify(callback).success();
        verifyNoMoreInteractions(callback);
        verifyNoMoreInteractions(call);
    }

    @Test
    public void successAfterOneRetry() {
        final ServiceCallback callback = mock(ServiceCallback.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).failure(new SocketException());
                return mock(ServiceCall.class);
            }
        }).doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).success();
                return mock(ServiceCall.class);
            }
        }).when(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, handler);
        simulateRetryAfterDelay(handler);
        retryer.sendAsync(null, null, null, callback);
        verifyDelay(handler, 0);
        verifyNoMoreInteractions(handler);
        verify(callback).success();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void retryOnceThenFail() {
        final HttpException expectedException = new HttpException(403);
        final ServiceCallback callback = mock(ServiceCallback.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).failure(new UnknownHostException());
                return mock(ServiceCall.class);
            }
        }).doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).failure(expectedException);
                return mock(ServiceCall.class);
            }
        }).when(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, handler);
        simulateRetryAfterDelay(handler);
        retryer.sendAsync(null, null, null, callback);
        verifyDelay(handler, 0);
        verifyNoMoreInteractions(handler);
        verify(callback).failure(any(Throwable.class));
        verify(callback).failure(expectedException);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void exhaustRetries() {
        final ServiceCall call = mock(ServiceCall.class);
        ServiceCallback callback = mock(ServiceCallback.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).failure(new HttpException(429));
                return call;
            }
        }).when(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, handler);
        simulateRetryAfterDelay(handler);
        retryer.sendAsync(null, null, null, callback);
        verifyDelay(handler, 0);
        verifyDelay(handler, 1);
        verifyDelay(handler, 2);
        verifyNoMoreInteractions(handler);
        verify(callback).failure(new HttpException(429));
        verifyNoMoreInteractions(callback);
        verifyNoMoreInteractions(call);
    }

    @Test
    public void cancel() throws InterruptedException {
        final ServiceCall call = mock(ServiceCall.class);
        ServiceCallback callback = mock(ServiceCallback.class);
        AvalancheIngestion ingestion = mock(AvalancheIngestion.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((ServiceCallback) invocationOnMock.getArguments()[3]).failure(new HttpException(503));
                return call;
            }
        }).when(ingestion).sendAsync(any(UUID.class), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        Handler handler = mock(Handler.class);
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, handler);
        retryer.sendAsync(null, null, null, callback).cancel();
        Thread.sleep(500);
        verifyNoMoreInteractions(callback);
        verify(call).cancel();
    }
}
