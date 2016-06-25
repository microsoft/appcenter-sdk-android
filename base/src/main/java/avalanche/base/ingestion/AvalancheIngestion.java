package avalanche.base.ingestion;

import avalanche.base.ingestion.models.LogContainer;

/**
 * The interface for AvalancheIngestion class.
 */
public interface AvalancheIngestion {

    /**
     * Send logs to the Ingestion service.
     *
     * @param logContainer    Payload.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     */
    void sendAsync(LogContainer logContainer, final ServiceCallback<Void> serviceCallback);

    void cancelAll();
}
