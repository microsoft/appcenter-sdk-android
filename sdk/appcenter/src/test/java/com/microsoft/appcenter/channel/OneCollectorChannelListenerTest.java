package com.microsoft.appcenter.channel;

import com.microsoft.appcenter.ingestion.models.Log;

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
    public void addGroupOnEnqueuingLog() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel);

        /* Enqueuing an event. */
        Log log = mock(Log.class);
        listener.onEnqueuingLog(log, TEST_GROUP);

        /* Verify group added. */
        verify(channel).addGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, null, null);

        /* Enqueuing an event to the one collector group. */
        listener.onEnqueuingLog(log, TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void clearGroupOnClear() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel);

        /* Clear a group. */
        listener.onClear(TEST_GROUP);

        /* Verify group added. */
        verify(channel).clear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);

        /* Clear the one collector group. */
        listener.onClear(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_SUFFIX);
        verifyNoMoreInteractions(channel);
    }
}
