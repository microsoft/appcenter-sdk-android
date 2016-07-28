package avalanche.core.channel;

import android.support.annotation.NonNull;

import avalanche.core.ingestion.models.Log;

/**
 * The interface for AvalancheChannel
 */
public interface AvalancheChannel {

    void addGroup(String groupName, int maxLogsPerBatch, int batchTimeInterval, int maxParallelBatches, Listener listener);

    /**
     * Add Log to queue to be persisted and sent.
     *
     * @param log       the Log to be enqueued
     * @param queueName the queue to use
     */
    void enqueue(@NonNull Log log, @NonNull String queueName);

    /**
     * Check whether channel is enabled or disabled.
     *
     * @return true if channel is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * Enable or disable channel.
     *
     * @param enabled true to enable, false to disable.
     */
    void setEnabled(boolean enabled);

    /**
     * Clear all persisted logs for the given group.
     *
     * @param groupName the group name.
     */
    void clear(String groupName);

    /**
     * Channel listener specification.
     */
    interface Listener {
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