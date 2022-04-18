/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;

import com.microsoft.appcenter.CancellationException;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.AppCenterIngestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.persistence.Persistence;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.microsoft.appcenter.channel.DefaultChannel.CLEAR_BATCH_SIZE;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultChannelRaceConditionTest extends AbstractDefaultChannelTest {

    @Test(timeout = 5000)
    public void disabledWhileHandlingIngestionSuccess() {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), anyCollection(), eq(1), anyList())).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), anyCollection(), eq(CLEAR_BATCH_SIZE), anyList())).then(getGetLogsAnswer(0));
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        when(mockIngestion.isEnabled()).thenReturn(true);
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        ((ServiceCallback) invocation.getArguments()[3]).onCallSucceeded(new HttpResponse(200, ""));
                        afterCallSemaphore.release();
                    }
                }.start();
                return mock(ServiceCall.class);
            }
        });

        /* Simulate enable module then disable. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, listener);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify handling success was ignored. */
        verify(listener, never()).onSuccess(any(Log.class));
        verify(listener).onFailure(any(Log.class), isA(CancellationException.class));
        verify(mockPersistence, never()).deleteLogs(anyString(), anyString());
    }

    @Test(timeout = 5000)
    public void disabledWhileHandlingIngestionFailure() {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), anyCollection(), eq(1), anyList())).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), anyCollection(), eq(CLEAR_BATCH_SIZE), anyList())).then(getGetLogsAnswer(0));
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        when(mockIngestion.isEnabled()).thenReturn(true);
        final Exception mockException = new IOException();
        when(mockIngestion.sendAsync(anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
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
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, listener);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify handling error was ignored. */
        verify(listener, never()).onFailure(any(Log.class), eq(mockException));
        verify(listener).onFailure(any(Log.class), isA(CancellationException.class));
    }
}
