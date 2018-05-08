package com.microsoft.appcenter.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * One Collector channel listener used to redirect selected traffic to One Collector.
 */
public class OneCollectorChannelListener implements Channel.Listener {

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    static final int ONE_COLLECTOR_TRIGGER_INTERVAL = 3 * 1000;

    /**
     * Number of metrics queue items which will trigger synchronization.
     */
    static final int ONE_COLLECTOR_TRIGGER_COUNT = 50;

    /**
     * Maximum number of requests being sent for the group.
     */
    static final int ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS = 3;

    /**
     * Postfix for One Collector's groups.
     */
    static final String ONE_COLLECTOR_GROUP_NAME_POSTFIX = "/one";

    private Channel mChannel;

    public OneCollectorChannelListener(@NonNull Channel channel) {
        mChannel = channel;
    }

    @Override
    public void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {
        if (isOneCollectorGroup(groupName)) {
            return;
        }
        String oneCollectorGroupName = getOneCollectorGroupName(groupName);
        mChannel.addGroup(oneCollectorGroupName, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, null);
    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        return false;
    }

    private String getOneCollectorGroupName(@NonNull String groupName) {
        return groupName + ONE_COLLECTOR_GROUP_NAME_POSTFIX;
    }

    private boolean isOneCollectorGroup(@NonNull String groupName) {
        return groupName.endsWith(ONE_COLLECTOR_GROUP_NAME_POSTFIX);
    }
}
