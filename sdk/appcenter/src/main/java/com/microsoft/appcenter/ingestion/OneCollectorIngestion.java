package com.microsoft.appcenter.ingestion;

import android.content.Context;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.http.DefaultHttpClient;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.appcenter.BuildConfig.VERSION_NAME;
import static com.microsoft.appcenter.http.DefaultHttpClient.CONTENT_TYPE_KEY;
import static com.microsoft.appcenter.http.DefaultHttpClient.CLIENT_VERSION_KEY;
import static com.microsoft.appcenter.http.DefaultHttpClient.CLIENT_VERSION_FORMAT;
import static com.microsoft.appcenter.http.DefaultHttpClient.UPLOAD_TIME_KEY;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;

public class OneCollectorIngestion implements Ingestion {

    /**
     * Default log URL.
     */
    private static final String DEFAULT_LOG_URL = "https://browser.events.data.microsoft.com/OneCollector/1.0";

    private static final String CONTENT_TYPE_VALUE = "application/x-json-stream; charset=utf-8";

    /**
     * API key header.
     */
    private static final String API_KEY = "apikey";

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
        Set<String> apiKeys = new HashSet<>();
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
        // TODO tag field would be useful to put enabled SDK modules in (e.g. for Analytis + Crashes + Distribute it could be ACD)
        headers.put(CLIENT_VERSION_KEY, String.format(CLIENT_VERSION_FORMAT, VERSION_NAME, "no"));

        /* Content encoding */
        // TODO

        /* Upload time */
        headers.put(UPLOAD_TIME_KEY, Long.toString(System.currentTimeMillis()));

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

    private static class IngestionCallTemplate implements HttpClient.CallTemplate {

        private final LogSerializer mLogSerializer;

        private final LogContainer mLogContainer;

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
                jsonStream.append('\n');
            }
            return jsonStream.toString();
        }

        @Override
        public void onBeforeCalling(URL url, Map<String, String> headers) {

        }
    }
}
