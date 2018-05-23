package com.microsoft.appcenter.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Log;

/**
 * The interface for Channel.
 */
public interface Channel {

    /**
     * Add a group for logs to be persisted and sent.
     *
     * @param groupName          the name of a group.
     * @param maxLogsPerBatch    maximum log count per batch.
     * @param batchTimeInterval  time interval for a next batch.
     * @param maxParallelBatches maximum number of batches in parallel.
     * @param ingestion          ingestion for the channel. If null then the default ingestion will be used.
     * @param groupListener      a listener for a service.
     */
    void addGroup(String groupName, int maxLogsPerBatch, long batchTimeInterval, int maxParallelBatches, Ingestion ingestion, GroupListener groupListener);

    /**
     * Remove a group for logs.
     *
     * @param groupName the name of a group.
     */
    void removeGroup(String groupName);

    /**
     * Add Log to queue to be persisted and sent.
     *
     * @param log       the Log to be enqueued.
     * @param groupName the group to use.
     */
    void enqueue(@NonNull Log log, @NonNull String groupName);

    /**
     * Check whether channel is enabled or disabled.
     *
     * @return true if channel is enabled, false otherwise.
     */
    @SuppressWarnings("unused")
    boolean isEnabled();

    /**
     * Enable or disable channel.
     *
     * @param enabled true to enable, false to disable.
     */
    void setEnabled(boolean enabled);

    /**
     * Update log URL.
     *
     * @param logUrl log URL.
     */
    void setLogUrl(String logUrl);

    /**
     * Clear all persisted logs for the given group.
     *
     * @param groupName the group name.
     */
    void clear(String groupName);

    /**
     * Invalidate device cache that this channel may have.
     */
    void invalidateDeviceCache();

    /**
     * Add a global listener to the channel.
     *
     * @param listener listener to add.
     */
    void addListener(Listener listener);

    /**
     * Remove a listener from the channel.
     *
     * @param listener listener to remove.
     */
    void removeListener(Listener listener);

    /**
     * Suspend channel and wait for a limited period of time for queued logs to be persisted.
     */
    void shutdown();

    /**
     * Channel global listener specification.
     */
    interface Listener {

        /**
         * Called whenever a new group is added.
         *
         * @param groupName group name.
         */
        void onGroupAdded(@NonNull String groupName);

        /**
         * Called whenever a new group is removed.
         *
         * @param groupName group name.
         */
        void onGroupRemoved(@NonNull String groupName);

        /**
         * Called whenever a log is being prepared.
         * This is used to alter some log properties if needed.
         * The channel might alter log furthermore between this event and the next one: {@link #shouldFilter}.
         *
         * @param log       log being enqueued.
         * @param groupName group of the log.
         */
        void onPreparingLog(@NonNull Log log, @NonNull String groupName);

        /**
         * Called after a log has been fully prepared and properties are now final.
         *
         * @param log       prepared log.
         * @param groupName group of the log.
         */
        void onPreparedLog(@NonNull Log log, @NonNull String groupName);

        /**
         * Called after a log has been fully prepared and properties are now final.
         * The specified log can be filtered out by listeners if at least one of them returns true.
         *
         * @param log log to filter out.
         * @return true to filter out the log, false to let it being stored and sent by the channel.
         */
        boolean shouldFilter(@NonNull Log log);

        /**
         * Called after channel state has changed.
         *
         * @param isEnabled new channel state.
         */
        void onGloballyEnabled(boolean isEnabled);

        /**
         * Called when a group is cleared.
         *
         * @param groupName The group name.
         */
        void onClear(@NonNull String groupName);
    }

    /**
     * Channel group listener specification.
     */
    interface GroupListener {

        /**
         * Called before processing a log.
         *
         * @param log The log that will be delivered.
         */
        void onBeforeSending(Log log);

        /**
         * Called when the log is delivered successfully.
         *
         * @param log The log that is delivered.
         */
        void onSuccess(Log log);

        /**
         * Called when the log is not delivered successfully.
         *
         * @param log The log that is not delivered.
         * @param e   The exception for failure.
         */
        void onFailure(Log log, Exception e);
    }
}
