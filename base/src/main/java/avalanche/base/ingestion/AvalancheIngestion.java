package avalanche.base.ingestion;

import java.util.UUID;

import avalanche.base.ingestion.models.LogContainer;

/**
 * The interface for AvalancheIngestion class.
 */
public interface AvalancheIngestion {

    /**
     * Send logs to the Ingestion service.
     *
     * @param appId application identifier.
     * @param installId install identifier.
     * @param logContainer payload.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link ServiceCall} object
     */
    ServiceCall sendAsync(String appId, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException;
}
