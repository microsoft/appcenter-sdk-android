package com.microsoft.azure.mobile.channel;

import android.content.Context;

import com.microsoft.azure.mobile.CancellationException;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.Ingestion;
import com.microsoft.azure.mobile.ingestion.IngestionHttp;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.persistence.Persistence;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static com.microsoft.azure.mobile.channel.DefaultChannel.CLEAR_BATCH_SIZE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class DefaultChannelRaceConditionTest extends AbstractDefaultChannelTest {

    @Test
    public void removeGroupWhileCounting() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        DatabasePersistenceAsync mockPersistenceAsync = mock(DatabasePersistenceAsync.class);
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {

                        /* Wait for add/remove calls to be done. */
                        beforeCallSemaphore.acquireUninterruptibly();

                        /* Return the count. */
                        DatabasePersistenceAsync.DatabasePersistenceAsyncCallback callback = (DatabasePersistenceAsync.DatabasePersistenceAsyncCallback) invocation.getArguments()[1];
                        callback.onSuccess(1);

                        /* Mark call as done. */
                        afterCallSemaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(mockPersistenceAsync).countLogs(anyString(), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));

        /* Simulate enable module then disable before persistence can return results. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mock(Ingestion.class), );
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mock(Channel.GroupListener.class));
        channel.removeGroup(TEST_GROUP);

        /* We wait on this object to make persistence return results after the groups add/remove calls. */
        beforeCallSemaphore.release();

        /* We wait on database call to return to verify behavior. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify we didn't get logs after counting since group was removed. */
        verify(mockPersistenceAsync, never()).getLogs(eq(TEST_GROUP), eq(1), anyListOf(Log.class), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));
    }

    @Test
    public void replaceGroupWhileCounting() throws Exception {

        /* Set up mocking. */
        final CountDownLatch latch = new CountDownLatch(1);
        final Semaphore semaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        DatabasePersistenceAsync mockPersistenceAsync = mock(DatabasePersistenceAsync.class);
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        doAnswer(new Answer<Object>() {

            private int count;

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final int callCount = ++count;
                new Thread() {

                    @Override
                    public void run() {

                        /* Wait for add/remove calls to be done. */
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        /* First call returns 1 then 2. */
                        DatabasePersistenceAsync.DatabasePersistenceAsyncCallback callback = (DatabasePersistenceAsync.DatabasePersistenceAsyncCallback) invocation.getArguments()[1];
                        callback.onSuccess(callCount);

                        /* Mark call as done. */
                        semaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(mockPersistenceAsync).countLogs(anyString(), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));

        /* Simulate enable module, disable, then re-enable before persistence can return results. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mock(Ingestion.class), );
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mock(Channel.GroupListener.class));
        channel.removeGroup(TEST_GROUP);

        /* We enable again with a different batching count to be able to check behavior on getLogs with different count values. */
        channel.addGroup(TEST_GROUP, 2, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mock(Channel.GroupListener.class));

        /* We wait on this object to make persistence return results after the groups add/remove calls. */
        latch.countDown();

        /* We wait on both database calls to return to verify behavior. */
        semaphore.acquireUninterruptibly(2);

        /* Verify only the second call has an effect, when the group was added a second time. */
        verify(mockPersistenceAsync, never()).getLogs(eq(TEST_GROUP), eq(1), anyListOf(Log.class), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));
        verify(mockPersistenceAsync).getLogs(eq(TEST_GROUP), eq(2), anyListOf(Log.class), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));
    }

    @Test
    public void disabledWhileCounting() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        DatabasePersistenceAsync mockPersistenceAsync = mock(DatabasePersistenceAsync.class);
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {

                        /* Wait for disable call to be done. */
                        beforeCallSemaphore.acquireUninterruptibly();

                        /* Return the count. */
                        DatabasePersistenceAsync.DatabasePersistenceAsyncCallback callback = (DatabasePersistenceAsync.DatabasePersistenceAsyncCallback) invocation.getArguments()[1];
                        callback.onSuccess(1);

                        /* Mark call as done. */
                        afterCallSemaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(mockPersistenceAsync).countLogs(anyString(), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));

        /* Simulate enable module then disable before persistence can return results. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mock(Ingestion.class), );
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, mock(Channel.GroupListener.class));
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* We wait on this object to make persistence return results after the groups add/remove calls. */
        beforeCallSemaphore.release();

        /* We wait on database call to return to verify behavior. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify we didn't get logs after counting since group was removed. */
        verify(mockPersistenceAsync, never()).getLogs(eq(TEST_GROUP), eq(1), anyListOf(Log.class), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));
    }

    @Test
    public void enableWhileDisabling() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), anyInt(), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        try {
                            invocation.callRealMethod();
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                        afterCallSemaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(mockPersistenceAsync).getLogs(anyString(), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));

        /* Simulate enable module then disable. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mock(IngestionHttp.class), );
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, listener);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Unblock call to getLogs. */
        beforeCallSemaphore.release();

        /* Wait call to getLogs. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Check canceled before we could delete logs. */
        verify(mockPersistence, never()).deleteLogs(TEST_GROUP);
    }

    @Test
    public void disabledWhileEnqueuing() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(0).thenReturn(1).thenReturn(0);
        when(mockPersistence.getLogs(anyString(), eq(1), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class))).then(getGetLogsAnswer(0));
        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        try {
                            invocation.callRealMethod();
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                        afterCallSemaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(mockPersistenceAsync).putLog(anyString(), any(Log.class), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));
        IngestionHttp mockIngestion = mock(IngestionHttp.class);

        /* Simulate enable module then disable. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, );
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, listener);
        channel.enqueue(mock(Log.class), TEST_GROUP);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify ingestion not sent. */
        verify(mockIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void disabledWhileTriggeringIngestion() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), eq(1), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class))).then(getGetLogsAnswer(0));
        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        try {
                            invocation.callRealMethod();
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                        afterCallSemaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(mockPersistenceAsync).getLogs(anyString(), eq(1), anyListOf(Log.class), any(DatabasePersistenceAsync.DatabasePersistenceAsyncCallback.class));
        IngestionHttp mockIngestion = mock(IngestionHttp.class);

        /* Simulate enable module then disable. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, );
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, listener);
        channel.removeGroup(TEST_GROUP);
        channel.addGroup(TEST_GROUP, 2, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, listener);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify ingestion not sent. */
        verify(mockIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void disabledWhileSendingLogs() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), eq(1), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class))).then(getGetLogsAnswer(0));
        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        IngestionHttp mockIngestion = mock(IngestionHttp.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        ((Runnable) invocation.getArguments()[0]).run();
                        afterCallSemaphore.release();
                    }
                }.start();
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Simulate enable module then disable. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, );
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, listener);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify ingestion not sent. */
        verify(mockIngestion, never()).sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void disabledWhileHandlingIngestionSuccess() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), eq(1), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class))).then(getGetLogsAnswer(0));
        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        IngestionHttp mockIngestion = mock(IngestionHttp.class);
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        ((ServiceCallback) invocation.getArguments()[3]).onCallSucceeded("");
                        afterCallSemaphore.release();
                    }
                }.start();
                return mock(ServiceCall.class);
            }
        });

        /* Simulate enable module then disable. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, );
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, listener);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify handling success was ignored. */
        verify(listener, never()).onSuccess(any(Log.class));
        verify(listener).onFailure(any(Log.class), argThat(new ArgumentMatcher<Exception>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof CancellationException;
            }
        }));
    }

    @Test
    public void disabledWhileHandlingIngestionFailure() throws Exception {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), eq(1), anyListOf(Log.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class))).then(getGetLogsAnswer(0));
        DatabasePersistenceAsync mockPersistenceAsync = spy(new DatabasePersistenceAsync(mockPersistence));
        whenNew(DatabasePersistenceAsync.class).withArguments(mockPersistence).thenReturn(mockPersistenceAsync);
        IngestionHttp mockIngestion = mock(IngestionHttp.class);
        final Exception mockException = new IOException();
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        ((ServiceCallback) invocation.getArguments()[3]).onCallFailed(mockException);
                        afterCallSemaphore.release();
                    }
                }.start();
                return mock(ServiceCall.class);
            }
        });

        /* Simulate enable module then disable. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUIDUtils.randomUUID().toString(), mockPersistence, mockIngestion, );
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, listener);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify handling error was ignored. */
        verify(listener, never()).onFailure(any(Log.class), eq(mockException));
        verify(listener).onFailure(any(Log.class), argThat(new ArgumentMatcher<Exception>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof CancellationException;
            }
        }));
    }
}
