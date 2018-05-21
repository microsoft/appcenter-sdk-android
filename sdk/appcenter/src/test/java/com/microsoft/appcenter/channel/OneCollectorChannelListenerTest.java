package com.microsoft.appcenter.channel;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.Extensions;
import com.microsoft.appcenter.ingestion.models.one.SdkExtension;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.Test;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class OneCollectorChannelListenerTest {

    @Test
    public void addCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

        /* Mock group added. */
        listener.onGroupAdded(TEST_GROUP);

        /* Verify one collector group added. */
        verify(channel).addGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, null, null);

        /* Mock one collector group added callback, should not loop indefinitely. */
        listener.onGroupAdded(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void removeCorrespondingGroup() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

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

        /* Mock a log. */
        CommonSchemaLog log1 = mock(CommonSchemaLog.class);
        Extensions ext1 = new Extensions();
        ext1.setSdk(new SdkExtension());
        when(log1.getExt()).thenReturn(ext1);

        /* Mock another log. */
        CommonSchemaLog log2 = mock(CommonSchemaLog.class);
        Extensions ext2 = new Extensions();
        ext2.setSdk(new SdkExtension());
        when(log2.getExt()).thenReturn(ext2);

        /* Mock conversion of logs. */
        Channel channel = mock(Channel.class);
        LogSerializer logSerializer = mock(LogSerializer.class);
        when(logSerializer.toCommonSchemaLog(any(Log.class))).thenReturn(Arrays.asList(log1, log2));

        /* Init listener. */
        UUID installId = UUIDUtils.randomUUID();
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel, logSerializer, installId);
        listener.onPreparedLog(mock(Log.class), TEST_GROUP);

        /* Verify same epoch. */
        assertNotNull(log1.getExt().getSdk().getEpoch());
        assertEquals(log1.getExt().getSdk().getEpoch(), log2.getExt().getSdk().getEpoch());

        /* Verify incremented sequence numbers. */
        assertEquals((Long) 1L, log1.getExt().getSdk().getSeq());
        assertEquals((Long) 2L, log2.getExt().getSdk().getSeq());

        /* Verify install ID set. */
        assertEquals(installId, log1.getExt().getSdk().getInstallId());
        assertEquals(installId, log2.getExt().getSdk().getInstallId());

        /* TODO verify enqueue here. */
    }

    @Test
    public void dontConvertCommonSchemaLogs() {

        /* Setup mocks. */
        Channel channel = mock(Channel.class);
        LogSerializer logSerializer = mock(LogSerializer.class);

        /* Init listener. */
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel, logSerializer, UUIDUtils.randomUUID());
        listener.onPreparedLog(mock(CommonSchemaLog.class), TEST_GROUP);

        /* Verify no conversion. */
        verify(logSerializer, never()).toCommonSchemaLog(any(Log.class));

        /* Verify no enqueuing. */
        verify(channel, never()).enqueue(any(Log.class), anyString());
    }

    @Test
    public void shouldFilterAppCenterLog() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

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
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel, mock(LogSerializer.class), UUIDUtils.randomUUID());

        /* Clear a group. */
        listener.onClear(TEST_GROUP);

        /* Verify group added. */
        verify(channel).clear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);

        /* Clear the one collector group. */
        listener.onClear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verifyNoMoreInteractions(channel);
    }
}
