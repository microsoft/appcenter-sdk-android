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
     * @param appSecret       a unique and secret key used to identify the application.
     * @param installId       install identifier.
     * @param logContainer    payload.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @return the {@link ServiceCall} object
     * @throws IllegalArgumentException thrown if callback is null
     */
    ServiceCall sendAsync(UUID appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException;
}
