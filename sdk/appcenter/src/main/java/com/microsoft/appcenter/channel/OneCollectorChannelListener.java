package com.microsoft.appcenter.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * One Collector channel listener used to redirect selected traffic to One Collector.
 */
public class OneCollectorChannelListener implements Channel.Listener {

    static final int ONE_COLLECTOR_TRIGGER_INTERVAL = 3 * 1000;

    static final int ONE_COLLECTOR_TRIGGER_COUNT = 50;

    static final int ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS = 3;

    static final String ONE_COLLECTOR_GROUP_NAME_POSTFIX = "/one";

    private Channel mChannel;

    public OneCollectorChannelListener(@NonNull Channel channel) {
        mChannel = channel;
    }

    @Override
    public void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {

        String oneCollectorGroupName = groupName + ONE_COLLECTOR_GROUP_NAME_POSTFIX;
        mChannel.addGroup(oneCollectorGroupName, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, null);
    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        return false;
    }
}
