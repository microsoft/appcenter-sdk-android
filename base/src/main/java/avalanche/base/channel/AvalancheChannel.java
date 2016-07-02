package avalanche.base.channel;

import android.support.annotation.NonNull;

import avalanche.base.ingestion.models.Log;

/**
 * The interface for AvalancheChannel
 */
public interface AvalancheChannel {

    /**
     * Add Log to queue to be persisted and sent.
     *
     * @param log the Log to be enqueued
     * @param queueName the queue to use
     */
    void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName);
}
