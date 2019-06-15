/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;

import com.microsoft.appcenter.CancellationException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.AppCenterIngestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.persistence.Persistence;
import com.microsoft.appcenter.utils.HandlerUtils;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.microsoft.appcenter.channel.DefaultChannel.CLEAR_BATCH_SIZE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;

public class DefaultChannelRaceConditionTest extends AbstractDefaultChannelTest {

    @Test(timeout = 5000)
    public void disabledWhileSendingLogs() {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), anyListOf(String.class), eq(1), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), anyListOf(String.class), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(0));
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(final InvocationOnMock invocation) {
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
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        Channel.GroupListener listener = mock(Channel.GroupListener.class);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, listener);
        channel.setEnabled(false);
        channel.setEnabled(true);

        /* Release call to mock ingestion. */
        beforeCallSemaphore.release();

        /* Wait for callback ingestion. */
        afterCallSemaphore.acquireUninterruptibly();

        /* Verify ingestion not sent. */
        verify(mockIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test(timeout = 5000)
    public void disabledWhileHandlingIngestionSuccess() {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), anyListOf(String.class), eq(1), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), anyListOf(String.class), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(0));
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        when(mockIngestion.sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("", null);
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
        verify(listener).onFailure(any(Log.class), argThat(new ArgumentMatcher<Exception>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof CancellationException;
            }
        }));
        verify(mockPersistence, never()).deleteLogs(anyString(), anyString());
    }

    @Test(timeout = 5000)
    public void disabledWhileHandlingIngestionFailure() {

        /* Set up mocking. */
        final Semaphore beforeCallSemaphore = new Semaphore(0);
        final Semaphore afterCallSemaphore = new Semaphore(0);
        Persistence mockPersistence = mock(Persistence.class);
        when(mockPersistence.countLogs(anyString())).thenReturn(1);
        when(mockPersistence.getLogs(anyString(), anyListOf(String.class), eq(1), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        when(mockPersistence.getLogs(anyString(), anyListOf(String.class), eq(CLEAR_BATCH_SIZE), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(0));
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        final Exception mockException = new IOException();
        when(mockIngestion.sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                new Thread() {

                    @Override
                    public void run() {
                        beforeCallSemaphore.acquireUninterruptibly();
                        ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(mockException);
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
        verify(listener).onFailure(any(Log.class), argThat(new ArgumentMatcher<Exception>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof CancellationException;
            }
        }));
    }
}
