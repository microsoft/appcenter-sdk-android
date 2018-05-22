package com.microsoft.appcenter.channel;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * One Collector channel listener used to redirect selected traffic to One Collector.
 */
public class OneCollectorChannelListener extends AbstractChannelListener {

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    @VisibleForTesting
    static final int ONE_COLLECTOR_TRIGGER_INTERVAL = 3 * 1000;

    /**
     * Number of metrics queue items which will trigger synchronization.
     */
    @VisibleForTesting
    static final int ONE_COLLECTOR_TRIGGER_COUNT = 50;

    /**
     * Maximum number of requests being sent for the group.
     */
    @VisibleForTesting
    static final int ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS = 3;

    /**
     * Postfix for One Collector's groups.
     */
    @VisibleForTesting
    static final String ONE_COLLECTOR_GROUP_NAME_SUFFIX = "/one";

    /**
     * Channel.
     */
    private Channel mChannel;

    /**
     * Init with channel.
     *
     * @param channel channel.
     */
    public OneCollectorChannelListener(@NonNull Channel channel) {
        mChannel = channel;
    }

    @Override
    public void onGroupAdded(@NonNull String groupName) {
        if (isOneCollectorGroup(groupName)) {
            return;
        }
        String oneCollectorGroupName = getOneCollectorGroupName(groupName);
        mChannel.addGroup(oneCollectorGroupName, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, null, null);
    }

    @Override
    public void onGroupRemoved(@NonNull String groupName) {
        if (isOneCollectorGroup(groupName)) {
            return;
        }
        String oneCollectorGroupName = getOneCollectorGroupName(groupName);
        mChannel.removeGroup(oneCollectorGroupName);
    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        return false;
    }

    @Override
    public void onClear(@NonNull String groupName) {
        if (isOneCollectorGroup(groupName)) {
            return;
        }
        String oneCollectorGroupName = getOneCollectorGroupName(groupName);
        mChannel.clear(oneCollectorGroupName);
    }

    /**
     * Get One Collector's group name for original one.
     *
     * @param groupName The group name.
     * @return The One Collector's group name.
     */
    private String getOneCollectorGroupName(@NonNull String groupName) {
        return groupName + ONE_COLLECTOR_GROUP_NAME_SUFFIX;
    }

    /**
     * Checks if the group has One Collector's postfix.
     *
     * @param groupName The group name.
     * @return true if group has One Collector's postfix, false otherwise.
     */
    private boolean isOneCollectorGroup(@NonNull String groupName) {
        return groupName.endsWith(ONE_COLLECTOR_GROUP_NAME_SUFFIX);
    }
}
