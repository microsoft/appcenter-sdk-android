package com.microsoft.appcenter.channel;

import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.Test;

import static com.microsoft.appcenter.channel.AbstractDefaultChannelTest.TEST_GROUP;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_GROUP_NAME_SUFFIX;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_COUNT;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_INTERVAL;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
