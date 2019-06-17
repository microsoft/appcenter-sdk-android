/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;

import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.AppCenterIngestion;
import com.microsoft.appcenter.ingestion.OneCollectorIngestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.persistence.Persistence;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static com.microsoft.appcenter.channel.DefaultChannel.START_TIMER_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class DefaultChannelPauseResumeTest extends AbstractDefaultChannelTest {

    @Test
    public void pauseResumeGroup() throws Persistence.PersistenceException {
        Persistence mockPersistence = mock(Persistence.class);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        Channel.GroupListener mockListener = mock(Channel.GroupListener.class);

        when(mockPersistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(50));
        when(mockIngestion.sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class))).then(getSendAsyncAnswer());

        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mockPersistence, mockIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mockListener);
        assertFalse(channel.getGroupState(TEST_GROUP).mPaused);

        /* Pause group. */
        channel.pauseGroup(TEST_GROUP, null);
        assertTrue(channel.getGroupState(TEST_GROUP).mPaused);

        /* Enqueue a log. */
        for (int i = 0; i < 50; i++) {
            channel.enqueue(mock(Log.class), TEST_GROUP, Flags.DEFAULTS);
        }
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));

        /* 50 logs are persisted but never being sent to Ingestion. */
        assertEquals(50, channel.getGroupState(TEST_GROUP).mPendingLogCount);
        verify(mockPersistence, times(50)).putLog(any(Log.class), eq(TEST_GROUP), eq(Flags.NORMAL));
        verify(mockIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence, never()).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, never()).onBeforeSending(any(Log.class));
        verify(mockListener, never()).onSuccess(any(Log.class));

        /* The counter should still be 50 now as we did NOT send data. */
        assertEquals(50, channel.getGroupState(TEST_GROUP).mPendingLogCount);

        /* Resume group. */
        channel.resumeGroup(TEST_GROUP, null);
        assertFalse(channel.getGroupState(TEST_GROUP).mPaused);

        /* Verify channel starts sending logs. */
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), eq(BATCH_TIME_INTERVAL));
        verify(mockIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mockPersistence).deleteLogs(any(String.class), any(String.class));
        verify(mockListener, times(50)).onBeforeSending(any(Log.class));
        verify(mockListener, times(50)).onSuccess(any(Log.class));

        /* The counter should be 0 now as we sent data. */
        assertEquals(0, channel.getGroupState(TEST_GROUP).mPendingLogCount);
    }

    @Test
    public void pauseGroupTwice() {
        DefaultChannel channel = spy(new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mock(Persistence.class), mock(AppCenterIngestion.class), mAppCenterHandler));
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mock(Channel.GroupListener.class));
        assertFalse(channel.getGroupState(TEST_GROUP).mPaused);

        /* Pause group twice. */
        channel.pauseGroup(TEST_GROUP, null);
        assertTrue(channel.getGroupState(TEST_GROUP).mPaused);
        channel.pauseGroup(TEST_GROUP, null);
        assertTrue(channel.getGroupState(TEST_GROUP).mPaused);

        /* Verify the group is paused only once. */
        verify(channel).cancelTimer(any(DefaultChannel.GroupState.class));
    }

    @Test
    public void resumeGroupWhileNotPaused() {
        DefaultChannel channel = spy(new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mock(Persistence.class), mock(AppCenterIngestion.class), mAppCenterHandler));
        channel.addGroup(TEST_GROUP, 50, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, null, mock(Channel.GroupListener.class));
        DefaultChannel.GroupState groupState = channel.getGroupState(TEST_GROUP);
        verify(channel).checkPendingLogs(groupState);
        assertFalse(groupState.mPaused);

        /* Resume group. */
        channel.resumeGroup(TEST_GROUP, null);
        assertFalse(groupState.mPaused);

        /* Verify resumeGroup doesn't resume the group while un-paused.  */
        verify(channel).checkPendingLogs(groupState);
    }

    @Test
    public void pauseResumeTargetToken() throws Persistence.PersistenceException {

        /* Mock database and ingestion. */
        Persistence persistence = mock(Persistence.class);
        OneCollectorIngestion ingestion = mock(OneCollectorIngestion.class);

        /* Create a channel with a log group that send logs 1 by 1. */
        AppCenterIngestion appCenterIngestion = mock(AppCenterIngestion.class);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), persistence, appCenterIngestion, mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, ingestion, null);

        /* Reset to verify further interactions besides initial check after adding group. */
        reset(persistence);

        /* Pause token. */
        String targetToken = "iKey-apiKey";
        channel.pauseGroup(TEST_GROUP, targetToken);

        /* Mock the database to return logs now. */
        when(persistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        when(persistence.countLogs(TEST_GROUP)).thenReturn(1);

        /* Enqueue a log. */
        Log log = mock(Log.class);
        when(log.getTransmissionTargetTokens()).thenReturn(Collections.singleton(targetToken));
        channel.enqueue(log, TEST_GROUP, Flags.DEFAULTS);

        /* Verify persisted but not incrementing and checking logs. */
        verify(persistence).putLog(log, TEST_GROUP, Flags.NORMAL);
        assertEquals(0, channel.getGroupState(TEST_GROUP).mPendingLogCount);
        verify(persistence, never()).countLogs(TEST_GROUP);
        verify(ingestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Pausing a second time has no effect. */
        channel.pauseGroup(TEST_GROUP, targetToken);
        verify(persistence, never()).countLogs(TEST_GROUP);

        /* Enqueueing a log from another transmission target works. */
        Log otherLog = mock(Log.class);
        when(otherLog.getTransmissionTargetTokens()).thenReturn(Collections.singleton("iKey2-apiKey2"));
        channel.enqueue(otherLog, TEST_GROUP, Flags.DEFAULTS);
        verify(ingestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        reset(ingestion);

        /* Resume token. */
        channel.resumeGroup(TEST_GROUP, targetToken);
        verify(ingestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Sending more logs works now. */
        reset(ingestion);
        channel.enqueue(log, TEST_GROUP, Flags.DEFAULTS);
        verify(ingestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Calling resume a second time has 0 effect. */
        reset(persistence);
        reset(ingestion);
        channel.resumeGroup(TEST_GROUP, targetToken);
        verifyZeroInteractions(persistence);
        verifyZeroInteractions(ingestion);

        /* AppCenter ingestion never used. */
        verify(appCenterIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void pauseGroupPauseTargetResumeGroupResumeTarget() throws Persistence.PersistenceException {

        /* Mock database and ingestion. */
        Persistence persistence = mock(Persistence.class);
        OneCollectorIngestion ingestion = mock(OneCollectorIngestion.class);

        /* Create a channel with a log group that send logs 1 by 1. */
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), persistence, mock(AppCenterIngestion.class), mAppCenterHandler);
        channel.addGroup(TEST_GROUP, 1, BATCH_TIME_INTERVAL, MAX_PARALLEL_BATCHES, ingestion, null);

        /* Pause group first. */
        channel.pauseGroup(TEST_GROUP, null);

        /* Pause token. */
        String targetToken = "iKey-apiKey";
        channel.pauseGroup(TEST_GROUP, targetToken);

        /* Mock the database to return logs now. */
        when(persistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        when(persistence.countLogs(TEST_GROUP)).thenReturn(1);

        /* Enqueue a log. */
        Log log = mock(Log.class);
        when(log.getTransmissionTargetTokens()).thenReturn(Collections.singleton(targetToken));
        channel.enqueue(log, TEST_GROUP, Flags.DEFAULTS);

        /* Verify persisted but not incrementing and checking logs. */
        verify(persistence).putLog(log, TEST_GROUP, Flags.NORMAL);
        assertEquals(0, channel.getGroupState(TEST_GROUP).mPendingLogCount);
        verify(ingestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Resume group should not send the log. */
        channel.resumeGroup(TEST_GROUP, null);
        verify(ingestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));

        /* Resume token, send the log now. */
        channel.resumeGroup(TEST_GROUP, targetToken);
        verify(ingestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void pauseResumeGroupWhenDisabled() {

        /* Create a channel with a log group. */
        DefaultChannel channel = spy(new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), mock(Persistence.class), mock(AppCenterIngestion.class), mAppCenterHandler));
        Channel.Listener listener = mock(Channel.Listener.class);
        channel.addListener(listener);

        /* Pause group. */
        channel.pauseGroup(TEST_GROUP, null);

        /* Verify channel doesn't do anything on pause. */
        verify(channel, never()).cancelTimer(any(DefaultChannel.GroupState.class));
        verify(listener, never()).onPaused(eq(TEST_GROUP), anyString());

        /* Resume group. */
        channel.resumeGroup(TEST_GROUP, null);

        /* Verify channel doesn't do anything on pause. */
        verify(channel, never()).checkPendingLogs(any(DefaultChannel.GroupState.class));
        verify(listener, never()).onResumed(eq(TEST_GROUP), anyString());
    }

    @Test
    public void pauseWithCustomIntervalAndResumeBeforeIntervalDue() {

        /* Mock current time. */
        long now = 1000;
        when(System.currentTimeMillis()).thenReturn(now);

        /* Create channel and group. */
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(TEST_GROUP)).thenReturn(0);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), persistence, mockIngestion, mAppCenterHandler);
        int batchTimeInterval = 10000;
        channel.addGroup(TEST_GROUP, 10, batchTimeInterval, MAX_PARALLEL_BATCHES, mockIngestion, mock(Channel.GroupListener.class));
        verifyStatic(never());
        SharedPreferencesManager.putLong(eq(START_TIMER_PREFIX + TEST_GROUP), anyLong());
        verify(mockIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), anyLong());

        /* When we enqueue a log while being paused. */
        channel.pauseGroup(TEST_GROUP, null);
        when(persistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        when(persistence.countLogs(TEST_GROUP)).thenReturn(1);
        channel.enqueue(mock(Log.class), TEST_GROUP, Flags.DEFAULTS);

        /* Verify that timer does not start but that the current time is saved for future reference. */
        verifyStatic();
        SharedPreferencesManager.putLong(eq(START_TIMER_PREFIX + TEST_GROUP), eq(now));
        verify(mockIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), anyLong());

        /* When we resume later (before interval is due) */
        when(SharedPreferencesManager.getLong(eq(START_TIMER_PREFIX + TEST_GROUP))).thenReturn(now);
        now = 3000;
        long expectedTimeToWait = 8000;
        when(System.currentTimeMillis()).thenReturn(now);
        channel.resumeGroup(TEST_GROUP, null);

        /* Check timer is started with remaining time. */
        verify(mAppCenterHandler).postDelayed(notNull(Runnable.class), eq(expectedTimeToWait));
        verify(mockIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }

    @Test
    public void pauseWithCustomIntervalAndResumeAfterIntervalDue() {

        /* Mock current time. */
        long now = 1000;
        when(System.currentTimeMillis()).thenReturn(now);

        /* Create channel and group. */
        Persistence persistence = mock(Persistence.class);
        when(persistence.countLogs(TEST_GROUP)).thenReturn(0);
        AppCenterIngestion mockIngestion = mock(AppCenterIngestion.class);
        DefaultChannel channel = new DefaultChannel(mock(Context.class), UUID.randomUUID().toString(), persistence, mockIngestion, mAppCenterHandler);
        int batchTimeInterval = 10000;
        channel.addGroup(TEST_GROUP, 10, batchTimeInterval, MAX_PARALLEL_BATCHES, mockIngestion, mock(Channel.GroupListener.class));
        verifyStatic(never());
        SharedPreferencesManager.putLong(eq(START_TIMER_PREFIX + TEST_GROUP), anyLong());
        verify(mockIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), anyLong());

        /* When we enqueue a log while being paused. */
        channel.pauseGroup(TEST_GROUP, null);
        when(persistence.getLogs(any(String.class), anyListOf(String.class), anyInt(), anyListOf(Log.class), any(Date.class), any(Date.class))).then(getGetLogsAnswer(1));
        when(persistence.countLogs(TEST_GROUP)).thenReturn(1);
        channel.enqueue(mock(Log.class), TEST_GROUP, Flags.DEFAULTS);

        /* Verify that timer does not start but that the current time is saved for future reference. */
        verifyStatic();
        SharedPreferencesManager.putLong(eq(START_TIMER_PREFIX + TEST_GROUP), eq(now));
        verify(mockIngestion, never()).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
        verify(mAppCenterHandler, never()).postDelayed(any(Runnable.class), anyLong());

        /* When we resume later (after interval is due) */
        when(SharedPreferencesManager.getLong(eq(START_TIMER_PREFIX + TEST_GROUP))).thenReturn(now);
        when(System.currentTimeMillis()).thenReturn(now + batchTimeInterval + 1);
        channel.resumeGroup(TEST_GROUP, null);

        /* Check timer is not started and logs send immediately. */
        verify(mAppCenterHandler, never()).postDelayed(notNull(Runnable.class), anyLong());
        verify(mockIngestion).sendAsync(anyString(), anyString(), any(UUID.class), any(LogContainer.class), any(ServiceCallback.class));
    }
}
