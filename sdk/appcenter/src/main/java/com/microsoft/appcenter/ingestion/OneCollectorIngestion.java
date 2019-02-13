package com.microsoft.appcenter.ingestion;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.TicketCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.http.DefaultHttpClient.CONTENT_TYPE_KEY;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;

public class OneCollectorIngestion implements Ingestion {

    /**
     * Default log URL.
     */
    private static final String DEFAULT_LOG_URL = "https://mobile.events.data.microsoft.com/OneCollector/1.0";

    /**
     * Content type header.
     */
    private static final String CONTENT_TYPE_VALUE = "application/x-json-stream; charset=utf-8";

    /**
     * API key header.
     */
    @VisibleForTesting
    static final String API_KEY = "apikey";

    /**
     * Tickets header.
     */
    @VisibleForTesting
    static final String TICKETS = "Tickets";

    /**
     * Strict authentication header ticket.
     */
    @VisibleForTesting
    static final String STRICT = "Strict";

    /**
     * Client version header key.
     */
    @VisibleForTesting
    static final String CLIENT_VERSION_KEY = "Client-Version";

    /**
     * Client version format string.
     */
    private static final String CLIENT_VERSION_FORMAT = "ACS-Android-Java-no-%s-no";

    /**
     * Upload time header key.
     */
    @VisibleForTesting
    static final String UPLOAD_TIME_KEY = "Upload-Time";

    /**
     * Log serializer.
     */
    private final LogSerializer mLogSerializer;

    /**
     * HTTP client.
     */
    private final HttpClient mHttpClient;

    /**
     * Log base URL (scheme + authority).
     */
    private String mLogUrl;

    /**
     * Init.
     *
     * @param context       any context.
     * @param logSerializer log serializer.
     */
    public OneCollectorIngestion(@NonNull Context context, @NonNull LogSerializer logSerializer) {
        mLogSerializer = logSerializer;
        mHttpClient = createHttpClient(context);
        mLogUrl = DEFAULT_LOG_URL;
    }

    @Override
    public ServiceCall sendAsync(String identityToken, String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {

        /* Gather API keys from logs. */
        Map<String, String> headers = new HashMap<>();
        Set<String> apiKeys = new LinkedHashSet<>();
        for (Log log : logContainer.getLogs()) {
            apiKeys.addAll(log.getTransmissionTargetTokens());
        }

        /* Build the header. String.join with iterable is only API level 26+. */
        StringBuilder apiKey = new StringBuilder();
        for (String targetToken : apiKeys) {
            apiKey.append(targetToken).append(",");
        }
        if (!apiKeys.isEmpty()) {
            apiKey.deleteCharAt(apiKey.length() - 1);
        }
        headers.put(API_KEY, apiKey.toString());

        /* Gather tokens from logs. */
        JSONObject tickets = new JSONObject();
        for (Log log : logContainer.getLogs()) {
            List<String> ticketKeys = ((CommonSchemaLog) log).getExt().getProtocol().getTicketKeys();
            if (ticketKeys != null) {
                for (String ticketKey : ticketKeys) {
                    String token = TicketCache.getTicket(ticketKey);
                    if (token != null) {
                        try {
                            tickets.put(ticketKey, token);
                        } catch (JSONException e) {
                            AppCenterLog.error(LOG_TAG, "Cannot serialize tickets, sending log anonymously", e);
                            break;
                        }
                    }
                }
            }
        }

        /* Pass ticket header if we have at least 1 token. */
        if (tickets.length() > 0) {
            headers.put(TICKETS, tickets.toString());

            /* Enable 400 errors on invalid tickets on debug builds. */
            if (Constants.APPLICATION_DEBUGGABLE) {
                headers.put(STRICT, Boolean.TRUE.toString());
            }
        }

        /* Content type. */
        headers.put(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);

        /* Client version (no import to avoid Javadoc issue). */
        String sdkVersion = com.microsoft.appcenter.BuildConfig.VERSION_NAME;
        headers.put(CLIENT_VERSION_KEY, String.format(CLIENT_VERSION_FORMAT, sdkVersion));

        /* Upload time */
        headers.put(UPLOAD_TIME_KEY, String.valueOf(System.currentTimeMillis()));

        /* Make the call. */
        HttpClient.CallTemplate callTemplate = new IngestionCallTemplate(mLogSerializer, logContainer);
        return mHttpClient.callAsync(mLogUrl, METHOD_POST, headers, callTemplate, serviceCallback);
    }

    /**
     * Update log URL.
     *
     * @param logUrl log URL.
     */
    @Override
    @SuppressWarnings("SameParameterValue")
    public void setLogUrl(@NonNull String logUrl) {
        mLogUrl = logUrl;
    }

    @Override
    public void reopen() {
        mHttpClient.reopen();
    }

    @Override
    public void close() throws IOException {
        mHttpClient.close();
    }

    /**
     * Call template implementation for One Collector.
     */
    private static class IngestionCallTemplate implements HttpClient.CallTemplate {

        /**
         * Log serializer.
         */
        private final LogSerializer mLogSerializer;

        /**
         * Log container.
         */
        private final LogContainer mLogContainer;

        /**
         * Init.
         */
        IngestionCallTemplate(LogSerializer logSerializer, LogContainer logContainer) {
            mLogSerializer = logSerializer;
            mLogContainer = logContainer;
        }

        @Override
        public String buildRequestBody() throws JSONException {

            /* Serialize payload. */
            StringBuilder jsonStream = new StringBuilder();
            for (Log log : mLogContainer.getLogs()) {
                jsonStream.append(mLogSerializer.serializeLog(log));

                /* We have to use a different delimiter specific to OneCollector. */
                jsonStream.append('\n');
            }
            return jsonStream.toString();
        }

        @Override
        public void onBeforeCalling(URL url, Map<String, String> headers) {
            if (AppCenterLog.getLogLevel() <= VERBOSE) {

                /* Log url. */
                AppCenterLog.verbose(LOG_TAG, "Calling " + url + "...");

                /* Log headers. */
                Map<String, String> logHeaders = new HashMap<>(headers);
                String apiKeys = logHeaders.get(API_KEY);
                if (apiKeys != null) {
                    logHeaders.put(API_KEY, HttpUtils.hideApiKeys(apiKeys));
                }
                String tickets = logHeaders.get(TICKETS);
                if (tickets != null) {
                    logHeaders.put(TICKETS, HttpUtils.hideTickets(tickets));
                }
                AppCenterLog.verbose(LOG_TAG, "Headers: " + logHeaders);
            }
        }
    }
}
