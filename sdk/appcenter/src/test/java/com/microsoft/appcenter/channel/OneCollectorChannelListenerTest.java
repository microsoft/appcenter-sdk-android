package com.microsoft.appcenter.channel;

import com.microsoft.appcenter.ingestion.models.Log;

import org.junit.Test;

import static com.microsoft.appcenter.channel.AbstractDefaultChannelTest.TEST_GROUP;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_GROUP_NAME_POSTFIX;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_COUNT;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_INTERVAL;
import static com.microsoft.appcenter.channel.OneCollectorChannelListener.ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OneCollectorChannelListenerTest {

    @Test
    public void addGroupOnEnqueuingLog() {
        Channel channel = mock(Channel.class);
        OneCollectorChannelListener listener = new OneCollectorChannelListener(channel);

        Log log = mock(Log.class);
        listener.onEnqueuingLog(log, TEST_GROUP);

        verify(channel).addGroup(TEST_GROUP + ONE_COLLECTOR_GROUP_NAME_POSTFIX, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, null);
    }
}
