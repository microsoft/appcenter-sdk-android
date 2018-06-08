package com.microsoft.appcenter.channel;

import android.content.Context;

import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.OneCollectorIngestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.Extensions;
import com.microsoft.appcenter.ingestion.models.one.SdkExtension;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static com.microsoft.appcenter.channel.AbstractDefaultChannelTest.TEST_GROUP;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_GROUP_NAME_SUFFIX;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_COUNT;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_INTERVAL;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

        /* Mock group added. */
        Channel.GroupListener groupListener = mock(Channel.GroupListener.class);
        listener.onGroupAdded(TEST_GROUP, groupListener);

        /* Verify one collector group added. */
        verify(channel).addGroup(eq(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX), eq(ONE_COLLECTOR_TRIGGER_COUNT), eq(ONE_COLLECTOR_TRIGGER_INTERVAL), eq(ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS), argThat(new ArgumentMatcher<Ingestion>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof OneCollectorIngestion;
            }
        }), same(groupListener));

        /* Mock one collector group added callback, should not loop indefinitely. */
        listener.onGroupAdded(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, groupListener);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void removeCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

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
        CommonSchemaLog log1 = mock(CommonSchemaLog.class);
        Extensions ext1 = new Extensions();
        ext1.setSdk(new SdkExtension());
        when(log1.getExt()).thenReturn(ext1);
        when(log1.getIKey()).thenReturn("t1");

        /* Mock another log. */
        CommonSchemaLog log2 = mock(CommonSchemaLog.class);
        Extensions ext2 = new Extensions();
        ext2.setSdk(new SdkExtension());
        when(log2.getExt()).thenReturn(ext2);
        when(log2.getIKey()).thenReturn("t1");

        /* Mock conversion of logs. */
        Channel channel = mock(Channel.class);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenReturn(Arrays.asList(log1, log2));

        /* Init listener. */
        UUID installId = UUIDUtils.randomUUID();
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, logSerializer, installId);
        listener.onPreparedLog(originalLog, TEST_GROUP);
        listener.onPreparedLog(mock(CommonSchemaLog.class), TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);

        /* Verify conversion. */
        verify(logSerializer).toCommonSchemaLog(originalLog);
        verifyNoMoreInteractions(logSerializer);

        /* Verify same epoch. */
        assertNotNull(log1.getExt().getSdk().getEpoch());
        assertEquals(log1.getExt().getSdk().getEpoch(), log2.getExt().getSdk().getEpoch());

        /* Verify incremented sequence numbers. */
        assertEquals((Long) 1L, log1.getExt().getSdk().getSeq());
        assertEquals((Long) 2L, log2.getExt().getSdk().getSeq());

        /* Verify install ID set. */
        assertEquals(installId, log1.getExt().getSdk().getInstallId());
        assertEquals(installId, log2.getExt().getSdk().getInstallId());

        /* Verify enqueue. */
        verify(channel).enqueue(log1, TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verify(channel).enqueue(log2, TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);

        /* We simulated that we see on prepared log on the enqueued log, verify no more enqueuing. */
        verify(channel, times(2)).enqueue(any(Log.class), anyString());

        /* Mock log with another key to see new seq/epoch. */
        when(originalLog.getTransmissionTargetTokens()).thenReturn(new HashSet<>(Collections.singletonList("t2")));
        CommonSchemaLog log3 = mock(CommonSchemaLog.class);
        Extensions ext3 = new Extensions();
        ext3.setSdk(new SdkExtension());
        when(log3.getExt()).thenReturn(ext3);
        when(log3.getIKey()).thenReturn("t2");
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenReturn(Collections.singletonList(log3));
        listener.onPreparedLog(originalLog, TEST_GROUP);
        assertEquals((Long) 1L, log3.getExt().getSdk().getSeq());
        assertNotNull(log3.getExt().getSdk().getEpoch());
        assertNotEquals(log1.getExt().getSdk().getEpoch(), log3.getExt().getSdk().getEpoch());

        /* Simulate disable/enable to reset epoch/seq. */
        listener.onGloballyEnabled(false);
        listener.onGloballyEnabled(true);

        /* Mock a 4rd log in first group to check reset. */
        CommonSchemaLog log4 = mock(CommonSchemaLog.class);
        Extensions ext4 = new Extensions();
        ext4.setSdk(new SdkExtension());
        when(log4.getExt()).thenReturn(ext4);
        when(log4.getIKey()).thenReturn("t2");
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenReturn(Collections.singletonList(log4));
        listener.onPreparedLog(originalLog, TEST_GROUP);

        /* Verify reset of epoch/seq. */
        assertEquals((Long) 1L, log4.getExt().getSdk().getSeq());
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
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, logSerializer, UUIDUtils.randomUUID());
        listener.onPreparedLog(log, TEST_GROUP);

        /* Verify conversion attempted. */
        verify(logSerializer).toCommonSchemaLog(any(Log.class));

        /* Verify no enqueuing as the log was invalid. */
        verify(channel, never()).enqueue(any(Log.class), anyString());
    }

    @Test
    public void dontConvertCommonSchemaLogs() {

        /* Setup mocks. */
        Channel channel = mock(Channel.class);
        LogSerializer logSerializer = mock(LogSerializer.class);

        /* Init listener. */
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, logSerializer, UUIDUtils.randomUUID());
        listener.onPreparedLog(mock(CommonSchemaLog.class), TEST_GROUP);

        /* Verify no conversion. */
        verify(logSerializer, never()).toCommonSchemaLog(any(Log.class));

        /* Verify no enqueuing. */
        verify(channel, never()).enqueue(any(Log.class), anyString());
    }

    @Test
    public void shouldFilterAppCenterLog() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

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
        OneCollectorChannelListener listener = new OneCollectorChannelListener(mock(Context.class), channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

        /* Clear a group. */
        listener.onClear(TEST_GROUP);

        /* Verify group added. */
        verify(channel).clear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);

        /* Clear the one collector group. */
        listener.onClear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verifyNoMoreInteractions(channel);
    }
}
