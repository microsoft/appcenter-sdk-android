package avalanche.core.ingestion;

import java.io.Closeable;
import java.util.UUID;

import avalanche.core.ingestion.models.LogContainer;

/**
 * The interface for AvalancheIngestion class.
 */
public interface AvalancheIngestion extends Closeable {

    /**
     * Send logs to the Ingestion service.
     *
     * @param appKey application identifier.
     * @param installId install identifier.
     * @param logContainer payload.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link ServiceCall} object
     */
    ServiceCall sendAsync(UUID appKey, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException;
}
