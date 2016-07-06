package avalanche.base.ingestion.http;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.utils.AvalancheLog;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AvalancheIngestionRetryerTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        AvalancheLog.setLogLevel(android.util.Log.VERBOSE);
    }

    @Test
    public void success() throws InterruptedException {
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
    public void successAfterOneRetry() throws InterruptedException {
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
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, 100);
        retryer.sendAsync(null, null, null, callback);
        Thread.sleep(40);
        verify(callback, times(0)).success();
        verify(callback, timeout(70)).success();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void retryOnceThenFail() throws InterruptedException {
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
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, 100, 200);
        retryer.sendAsync(null, null, null, callback);
        Thread.sleep(40);
        verify(callback, times(0)).failure(any(Throwable.class));
        verify(callback, timeout(70)).failure(expectedException);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void exhaustRetries() throws InterruptedException {
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
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, 100, 200);
        retryer.sendAsync(null, null, null, callback);
        Thread.sleep(500);
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
        AvalancheIngestion retryer = new AvalancheIngestionRetryer(ingestion, 100, 200);
        retryer.sendAsync(null, null, null, callback).cancel();
        Thread.sleep(500);
        verifyNoMoreInteractions(callback);
        verify(call).cancel();
    }
}
