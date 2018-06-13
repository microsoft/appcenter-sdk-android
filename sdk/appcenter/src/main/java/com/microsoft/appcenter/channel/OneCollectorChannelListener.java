package com.microsoft.appcenter.channel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.OneCollectorIngestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.SdkExtension;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.UUIDUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * One Collector channel listener used to redirect selected traffic to One Collector.
 */
public class OneCollectorChannelListener extends AbstractChannelListener {

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    @VisibleForTesting
    static final long ONE_COLLECTOR_TRIGGER_INTERVAL = 3 * 1000;

    /**
     * Number of metrics queue items which will trigger synchronization.
     */
    @VisibleForTesting
    static final int ONE_COLLECTOR_TRIGGER_COUNT = 50;

    /**
     * Maximum number of requests being sent for the group.
     */
    @VisibleForTesting
    static final int ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS = 2;

    /**
     * Postfix for One Collector's groups.
     */
    @VisibleForTesting
    static final String ONE_COLLECTOR_GROUP_NAME_SUFFIX = "/one";

    /**
     * Channel.
     */
    private final Channel mChannel;

    /**
     * Log serializer.
     */
    private final LogSerializer mLogSerializer;

    /**
     * Install id.
     */
    private final UUID mInstallId;

    /**
     * Ingestion instance.
     */
    private final Ingestion mIngestion;

    /**
     * Epochs and sequences grouped by iKey.
     */
    private final Map<String, EpochAndSeq> mEpochsAndSeqsByIKey = new HashMap<>();

    /**
     * Init with channel.
     *
     * @param context context.
     * @param channel channel.
     */
    public OneCollectorChannelListener(@NonNull Context context, @NonNull Channel channel, @NonNull LogSerializer logSerializer, @NonNull UUID installId) {
        mChannel = channel;
        mLogSerializer = logSerializer;
        mInstallId = installId;
        mIngestion = new OneCollectorIngestion(context, mLogSerializer);
    }

    @Override
    public void onGroupAdded(@NonNull String groupName, Channel.GroupListener groupListener) {
        if (isOneCollectorGroup(groupName)) {
            return;
        }
        String oneCollectorGroupName = getOneCollectorGroupName(groupName);
        mChannel.addGroup(oneCollectorGroupName, ONE_COLLECTOR_TRIGGER_COUNT, ONE_COLLECTOR_TRIGGER_INTERVAL, ONE_COLLECTOR_TRIGGER_MAX_PARALLEL_REQUESTS, mIngestion, groupListener);
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
    public void onPreparedLog(@NonNull Log log, @NonNull String groupName) {

        /* Nothing to do on common schema log prepared. */
        if (!isOneCollectorCompatible(log)) {
            return;
        }

        /* Convert logs to Common Schema. */
        Collection<CommonSchemaLog> commonSchemaLogs;
        try {
            commonSchemaLogs = mLogSerializer.toCommonSchemaLog(log);
        } catch (IllegalArgumentException e) {
            AppCenterLog.error(LOG_TAG, "Cannot send a log to one collector: " + e.getMessage());
            return;
        }

        /* Add SDK extension part A fields. libVer is already set. */
        for (CommonSchemaLog commonSchemaLog : commonSchemaLogs) {
            EpochAndSeq epochAndSeq = mEpochsAndSeqsByIKey.get(commonSchemaLog.getIKey());
            if (epochAndSeq == null) {
                epochAndSeq = new EpochAndSeq(UUIDUtils.randomUUID().toString());
                mEpochsAndSeqsByIKey.put(commonSchemaLog.getIKey(), epochAndSeq);
            }
            SdkExtension sdk = commonSchemaLog.getExt().getSdk();
            sdk.setEpoch(epochAndSeq.epoch);
            sdk.setSeq(++epochAndSeq.seq);
            sdk.setInstallId(mInstallId);
        }

        /* Enqueue logs to one collector group. */
        String oneCollectorGroupName = getOneCollectorGroupName(groupName);
        for (CommonSchemaLog commonSchemaLog : commonSchemaLogs) {
            mChannel.enqueue(commonSchemaLog, oneCollectorGroupName);
        }
    }

    @Override
    public boolean shouldFilter(@NonNull Log log) {

        /* Don't send the logs to AppCenter if it is being sent to OneCollector. */
        return isOneCollectorCompatible(log);
    }

    /**
     * Get One Collector's group name for original one.
     *
     * @param groupName The group name.
     * @return The One Collector's group name.
     */
    private static String getOneCollectorGroupName(@NonNull String groupName) {
        return groupName + ONE_COLLECTOR_GROUP_NAME_SUFFIX;
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
     * Checks if the group has One Collector's postfix.
     *
     * @param groupName The group name.
     * @return true if group has One Collector's postfix, false otherwise.
     */
    private static boolean isOneCollectorGroup(@NonNull String groupName) {
        return groupName.endsWith(ONE_COLLECTOR_GROUP_NAME_SUFFIX);
    }

    /**
     * Checks if the log is compatible with One Collector.
     *
     * @param log The log.
     * @return true if the log is compatible with One Collector, false otherwise.
     */
    private static boolean isOneCollectorCompatible(@NonNull Log log) {
        return !(log instanceof CommonSchemaLog) && !log.getTransmissionTargetTokens().isEmpty();
    }

    @Override
    public void onGloballyEnabled(boolean isEnabled) {
        if (!isEnabled) {
            mEpochsAndSeqsByIKey.clear();
        }
    }

    /**
     * Epoch and sequence number for logs.
     */
    private static class EpochAndSeq {

        /**
         * Epoch.
         */
        final String epoch;

        /**
         * Sequence number.
         */
        long seq;

        /**
         * Init.
         */
        EpochAndSeq(String epoch) {
            this.epoch = epoch;
        }
    }
}
