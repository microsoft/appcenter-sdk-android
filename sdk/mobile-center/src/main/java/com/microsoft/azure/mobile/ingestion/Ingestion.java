package com.microsoft.azure.mobile.ingestion;

import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;

import java.io.Closeable;
import java.util.UUID;

/**
 * Interface to send logs to the Ingestion service.
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
    ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException;

    /**
     * Update log URL.
     *
     * @param logUrl log URL.
     */
    void setLogUrl(String logUrl);
}
