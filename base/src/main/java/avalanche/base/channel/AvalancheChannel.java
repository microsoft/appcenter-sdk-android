package avalanche.base.channel;

import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.Log;

/**
 * The interface for AvalancheChannel
 */
public interface AvalancheChannel extends ServiceCallback {

    /**
     * Add Log to queue to be persisted and sent with regular priority.
     *
     * @param log the Log to be enqueued
     */
    void enqueue(Log log);

    /**
     * Add Log to queue to be persisted and sent.
     *
     * @param log the Log to be enqueued
     * @param priority the priority for the item
     */
    void enqueue(Log log, @ChannelPriorityDef int priority);
}
