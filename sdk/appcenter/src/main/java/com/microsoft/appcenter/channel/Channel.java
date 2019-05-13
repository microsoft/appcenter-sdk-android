/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.channel;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Log;

import static com.microsoft.appcenter.Flags.CRITICAL;
import static com.microsoft.appcenter.Flags.NORMAL;

/**
 * The interface for Channel.
 */
public interface Channel {

    /**
     * Set app secret. Intended usage is to use that only if there was no app secret at initialization time.
     * The behavior is undefined if trying to update app secret a second time.
     *
     * @param appSecret app secret.
     */
    void setAppSecret(@NonNull String appSecret);

    /**
     * Set maximum SQLite database size.
     *
     * @param maxStorageSizeInBytes maximum SQLite database size in bytes.
     * @return true if database size was set, otherwise false.
     */
    boolean setMaxStorageSize(long maxStorageSizeInBytes);

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
     * Pauses the given group.
     *
     * @param groupName   the name of a group.
     * @param targetToken the target token to pause, or null to pause the entire group.
     */
    void pauseGroup(String groupName, String targetToken);

    /**
     * Resumes transmission for the given group.
     *
     * @param groupName   the name of a group.
     * @param targetToken the target token to resume, or null to resume the entire group.
     */
    void resumeGroup(String groupName, String targetToken);

    /**
     * Add log to queue to be persisted and sent.
     *
     * @param log       the log to be enqueued.
     * @param groupName the group to use.
     * @param flags     the flags for this log.
     */
    void enqueue(@NonNull Log log,
                 @NonNull String groupName,
                 @IntRange(from = NORMAL, to = CRITICAL) int flags);

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
         * @param groupName     group name.
         * @param groupListener group listener.
         */
        void onGroupAdded(@NonNull String groupName, GroupListener groupListener, long batchTimeInterval);

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
        @SuppressWarnings("unused")
        void onPreparingLog(@NonNull Log log, @NonNull String groupName);

        /**
         * Called after a log has been fully prepared and properties are now final.
         *
         * @param log       prepared log.
         * @param groupName group of the log.
         * @param flags     log flags.
         */
        void onPreparedLog(@NonNull Log log, @NonNull String groupName, int flags);

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

        /**
         * Called when a group is paused.
         *
         * @param groupName   The group name.
         * @param targetToken The target token is paused, or null when the entire group is paused.
         */
        void onPaused(@NonNull String groupName, String targetToken);

        /**
         * Called when a group is resumed.
         *
         * @param groupName   The group name.
         * @param targetToken The target token is resumed, or null when the entire group is resumed.
         */
        void onResumed(@NonNull String groupName, String targetToken);
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
