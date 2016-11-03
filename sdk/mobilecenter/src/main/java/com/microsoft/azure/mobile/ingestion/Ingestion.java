package com.microsoft.azure.mobile.ingestion;

import com.microsoft.azure.mobile.ingestion.models.LogContainer;

import java.io.Closeable;
import java.util.UUID;

/**
 * The interface for Ingestion class.
 */
public interface Ingestion extends Closeable {

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

    /**
     * Update server url.
     *
     * @param serverUrl server url.
     */
    void setServerUrl(String serverUrl);
}
