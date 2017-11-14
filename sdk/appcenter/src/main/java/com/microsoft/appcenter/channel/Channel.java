package com.microsoft.appcenter.channel;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;

/**
 * The interface for Channel
 */
public interface Channel {

    /**
     * Add a group for logs to be persisted and sent.
     *
     * @param groupName          the name of a group.
     * @param maxLogsPerBatch    maximum log count per batch.
     * @param batchTimeInterval  time interval for a next batch.
     * @param maxParallelBatches maximum number of batches in parallel.
     * @param groupListener      a listener for a service.
     */
    void addGroup(String groupName, int maxLogsPerBatch, long batchTimeInterval, int maxParallelBatches, GroupListener groupListener);

    /**
     * Remove a group for logs.
     *
     * @param groupName the name of a group.
     */
    void removeGroup(String groupName);

    /**
     * Add Log to queue to be persisted and sent.
     *
     * @param log       the Log to be enqueued
     * @param groupName the group to use
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
         * Called whenever a log is enqueued.
         *
         * @param log       log being enqueued.
         * @param groupName group of the log.
         */
        @SuppressWarnings("UnusedParameters")
        void onEnqueuingLog(@NonNull Log log, @NonNull String groupName);
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