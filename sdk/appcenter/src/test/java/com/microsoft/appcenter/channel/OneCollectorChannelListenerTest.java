/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.content.Context;

import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.OneCollectorIngestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.Extensions;
import com.microsoft.appcenter.ingestion.models.one.MockCommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.SdkExtension;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static com.microsoft.appcenter.Flags.CRITICAL;
import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.channel.AbstractDefaultChannelTest.TEST_GROUP;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_GROUP_NAME_SUFFIX;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_COUNT;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class OneCollectorChannelListenerTest {

    @Test
    public void addCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUID.randomUUID());

        /* Mock group added. */
        long batchTimeInterval = 3000;
        Channel.GroupListener groupListener = mock(Channel.GroupListener.class);
        listener.onGroupAdded(TEST_GROUP, groupListener, batchTimeInterval);

        /* Verify one collector group added. */
        verify(channel).addGroup(eq(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX), eq(ONE_COLLECTOR_TRIGGER_COUNT), eq(batchTimeInterval), eq(ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS), argThat(new ArgumentMatcher<Ingestion>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof OneCollectorIngestion;
            }
        }), same(groupListener));

        /* Mock one collector group added callback, should not loop indefinitely. */
        listener.onGroupAdded(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, groupListener, batchTimeInterval);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void removeCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUID.randomUUID());

        /* Mock group removed. */
        listener.onGroupRemoved(TEST_GROUP);

        /* Verify one collector group added. */
        verify(channel).removeGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);

        /* Mock one collector group added callback, should not loop indefinitely. */
        listener.onGroupRemoved(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void enqueueConvertedLogs() {

        /* Mock original log. */
        Log originalLog = mock(Log.class);
        when(originalLog.getTransmissionTargetTokens()).thenReturn(new HashSet<>(Collections.singletonList("t1")));

        /* Mock a log. */
        CommonSchemaLog log1 = new MockCommonSchemaLog();
        log1.setIKey("t1");
        Extensions ext1 = new Extensions();
        ext1.setSdk(new SdkExtension());
        log1.setExt(ext1);

        /* Mock another log. */
        CommonSchemaLog log2 = new MockCommonSchemaLog();
        log2.setIKey("t1");
        Extensions ext2 = new Extensions();
        ext2.setSdk(new SdkExtension());
        log2.setExt(ext2);

        /* Mock conversion of logs. */
        Channel channel = mock(Channel.class);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenReturn(Arrays.asList(log1, log2));

        /* Init listener. */
        UUID installId = UUID.randomUUID();
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, logSerializer, installId);
        listener.onPreparedLog(originalLog, TEST_GROUP, DEFAULTS);
        listener.onPreparedLog(mock(CommonSchemaLog.class), TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, DEFAULTS);

        /* Verify conversion. */
        verify(logSerializer).toCommonSchemaLog(originalLog);
        verifyNoMoreInteractions(logSerializer);

        /* Verify flags. */
        assertEquals(Long.valueOf(DEFAULTS), log1.getFlags());
        assertEquals(Long.valueOf(DEFAULTS), log2.getFlags());

        /* Verify same epoch. */
        assertNotNull(log1.getExt().getSdk().getEpoch());
        assertEquals(log1.getExt().getSdk().getEpoch(), log2.getExt().getSdk().getEpoch());

        /* Verify incremented sequence numbers. */
        assertEquals(Long.valueOf(1), log1.getExt().getSdk().getSeq());
        assertEquals(Long.valueOf(2), log2.getExt().getSdk().getSeq());

        /* Verify install ID set. */
        assertEquals(installId, log1.getExt().getSdk().getInstallId());
        assertEquals(installId, log2.getExt().getSdk().getInstallId());

        /* Verify enqueue. */
        verify(channel).enqueue(log1, TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, DEFAULTS);
        verify(channel).enqueue(log2, TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, DEFAULTS);

        /* We simulated that we see on prepared log on the enqueued log, verify no more enqueuing. */
        verify(channel, times(2)).enqueue(any(Log.class), anyString(), eq(DEFAULTS));

        /* Mock log with another key to see new seq/epoch. */
        when(originalLog.getTransmissionTargetTokens()).thenReturn(new HashSet<>(Collections.singletonList("t2")));
        CommonSchemaLog log3 = new MockCommonSchemaLog();
        log3.setIKey("t2");
        Extensions ext3 = new Extensions();
        ext3.setSdk(new SdkExtension());
        log3.setExt(ext3);
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenReturn(Collections.singletonList(log3));
        listener.onPreparedLog(originalLog, TEST_GROUP, CRITICAL);
        assertEquals(Long.valueOf(CRITICAL), log3.getFlags());
        assertEquals(Long.valueOf(1), log3.getExt().getSdk().getSeq());
        assertNotNull(log3.getExt().getSdk().getEpoch());
        assertNotEquals(log1.getExt().getSdk().getEpoch(), log3.getExt().getSdk().getEpoch());

        /* Simulate disable/enable to reset epoch/seq. */
        listener.onGloballyEnabled(false);
        listener.onGloballyEnabled(true);

        /* Mock a 4rd log in first group to check reset. */
        CommonSchemaLog log4 = new MockCommonSchemaLog();
        log4.setIKey("t2");
        Extensions ext4 = new Extensions();
        ext4.setSdk(new SdkExtension());
        log4.setExt(ext4);
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenReturn(Collections.singletonList(log4));
        listener.onPreparedLog(originalLog, TEST_GROUP, NORMAL);

        /* Verify flags and reset of epoch/seq. */
        assertEquals(Long.valueOf(NORMAL), log4.getFlags());
        assertEquals(Long.valueOf(1), log4.getExt().getSdk().getSeq());
        assertNotNull(log4.getExt().getSdk().getEpoch());
        assertNotEquals(log3.getExt().getSdk().getEpoch(), log4.getExt().getSdk().getEpoch());
    }

    @Test
    public void validateCommonSchemaLogs() {

        /* Setup mocks. */
        Channel channel = mock(Channel.class);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenThrow(new IllegalArgumentException());
        Log log = mock(Log.class);
        when(log.getTransmissionTargetTokens()).thenReturn(Collections.singleton("token"));

        /* Init listener. */
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, logSerializer, UUID.randomUUID());
        listener.onPreparedLog(log, TEST_GROUP, DEFAULTS);

        /* Verify conversion attempted. */
        verify(logSerializer).toCommonSchemaLog(any(Log.class));

        /* Verify no enqueuing as the log was invalid. */
        verify(channel, never()).enqueue(any(Log.class), anyString(), anyInt());
    }

    @Test
    public void dontConvertCommonSchemaLogs() {

        /* Setup mocks. */
        Channel channel = mock(Channel.class);
        LogSerializer logSerializer = mock(LogSerializer.class);

        /* Init listener. */
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, logSerializer, UUID.randomUUID());
        listener.onPreparedLog(mock(CommonSchemaLog.class), TEST_GROUP, DEFAULTS);

        /* Verify no conversion. */
        verify(logSerializer, never()).toCommonSchemaLog(any(Log.class));

        /* Verify no enqueuing. */
        verify(channel, never()).enqueue(any(Log.class), anyString(), anyInt());
    }

    @Test
    public void shouldFilterAppCenterLog() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUID.randomUUID());

        /* App center log with no transmission target must not be filtered. */
        Log log = mock(Log.class);
        assertFalse(listener.shouldFilter(log));

        /* App center log with transmission target must be filtered. */
        when(log.getTransmissionTargetTokens()).thenReturn(new HashSet<>(Collections.singletonList("token")));
        assertTrue(listener.shouldFilter(log));

        /* Common schema logs never filtered out by this listener if no validation. */
        assertFalse(listener.shouldFilter(mock(CommonSchemaLog.class)));
    }

    @Test
    public void clearCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUID.randomUUID());

        /* Clear a group. */
        listener.onClear(TEST_GROUP);

        /* Verify group added. */
        verify(channel).clear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);

        /* Clear the one collector group: nothing more happens. */
        listener.onClear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void pauseCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUID.randomUUID());

        /* Pause a group. */
        listener.onPaused(TEST_GROUP, null);

        /* Verify group paused. */
        verify(channel).pauseGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, null);

        /* Pause the one collector group: nothing more happens. */
        listener.onPaused(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, null);
        verifyNoMoreInteractions(channel);

        /* Pause a single token. */
        listener.onPaused(TEST_GROUP, "token");

        /* Verify. */
        verify(channel).pauseGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, "token");
        listener.onPaused(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, null);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void resumeCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUID.randomUUID());

        /* Resume a group. */
        listener.onResumed(TEST_GROUP, null);

        /* Verify group resumed. */
        verify(channel).resumeGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, null);

        /* Resume the one collector group: nothing more happens. */
        listener.onResumed(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, null);
        verifyNoMoreInteractions(channel);

        /* Resume a single token. */
        listener.onResumed(TEST_GROUP, "token");

        /* Verify. */
        verify(channel).resumeGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, "token");
        listener.onResumed(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, null);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void setLogUrl() {
        OneCollectorIngestion ingestion = mock(OneCollectorIngestion.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(ingestion, mock(Channel.class), mock(LogSerializer.class), UUID.randomUUID());

        /* Set the log url. */
        String logUrl = "http://mock";
        listener.setLogUrl(logUrl);
        verify(ingestion).setLogUrl(logUrl);
    }
}
