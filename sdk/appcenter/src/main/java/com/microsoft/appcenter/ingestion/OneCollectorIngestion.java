package com.microsoft.appcenter.ingestion;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.BuildConfig.VERSION_NAME;
import static com.microsoft.appcenter.http.DefaultHttpClient.CONTENT_TYPE_KEY;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;

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
        HttpClientRetryer retryer = new HttpClientRetryer(new DefaultHttpClient());
        NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(context);
        mHttpClient = new HttpClientNetworkStateHandler(retryer, networkStateHelper);
        mLogUrl = DEFAULT_LOG_URL;
    }

    @Override
    public ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {

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

        /* Content type. */
        headers.put(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);

        /* Client version */
        headers.put(CLIENT_VERSION_KEY, String.format(CLIENT_VERSION_FORMAT, VERSION_NAME));

        /* Content encoding */
        // TODO Android HttpUrlConnection should take care of this?  Need to verify in fiddler

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

                    /* TODO possibly censor multiple API keys individually instead of whole set */
                    logHeaders.put(API_KEY, HttpUtils.hideSecret(apiKeys));
                }
                AppCenterLog.verbose(LOG_TAG, "Headers: " + logHeaders);
            }
        }
    }
}
