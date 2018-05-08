package com.microsoft.appcenter.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * One Collector channel listener used to redirect selected traffic to One Collector.
 */
public class OneCollectorChannelListener implements Channel.Listener {

    private static final int ONE_COLLECTOR_TRIGGER_INTERVAL = 3 * 1000;

    private static final int ONE_COLLECTOR_TRIGGER_COUNT = 50;

    private static final int ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS = 3;

    private Channel mChannel;

    public OneCollectorChannelListener(@NonNull Channel channel) {
        mChannel = channel;
    }

    @Override
    public void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {

        String oneCollectorGroupName = groupName + "/one";
        mChannel.addGroup(oneCollectorGroupName, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, null);
    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        return false;
    }
}
