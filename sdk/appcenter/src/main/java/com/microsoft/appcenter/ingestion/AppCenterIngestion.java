package com.microsoft.appcenter.ingestion;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;

public class AppCenterIngestion implements Ingestion {

    /**
     * Default log URL.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_LOG_URL = "https://in.appcenter.ms";

    /**
     * API Path.
     */
    @VisibleForTesting
    static final String API_PATH = "/logs?api-version=1.0.0";

    /**
     * Installation identifier HTTP Header.
     */
    @VisibleForTesting
    static final String INSTALL_ID = "Install-ID";

    /**
     * Application secret HTTP Header.
     */
    @VisibleForTesting
    static final String APP_SECRET = "App-Secret";

    /**
     * Auth token format for Authorization header.
     */
    @VisibleForTesting
    static final String AUTH_TOKEN_FORMAT = "Bearer %s";

    /**
     * Authorization HTTP Header.
     */
    @VisibleForTesting
    static final String AUTHORIZATION_HEADER = "Authorization";

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
     * Authentication token current value.
     */
    private String mAuthToken;

    /**
     * Init.
     *
     * @param context       any context.
     * @param logSerializer log serializer.
     */
    public AppCenterIngestion(@NonNull Context context, @NonNull LogSerializer logSerializer) {
        mLogSerializer = logSerializer;
        mHttpClient = createHttpClient(context);
        mLogUrl = DEFAULT_LOG_URL;
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
    public void setAuthToken(@NonNull String authToken) {
        mAuthToken = authToken;
    }

    @Override
    public String getAuthToken() {
        return mAuthToken;
    }

    @Override
    public ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, final ServiceCallback serviceCallback) throws IllegalArgumentException {
        Map<String, String> headers = new HashMap<>();
        headers.put(INSTALL_ID, installId.toString());
        headers.put(APP_SECRET, appSecret);
        if (mAuthToken != null && mAuthToken.length() > 0) {
            headers.put(AUTHORIZATION_HEADER, String.format(AUTH_TOKEN_FORMAT, mAuthToken));
        }
        HttpClient.CallTemplate callTemplate = new IngestionCallTemplate(mLogSerializer, logContainer);
        return mHttpClient.callAsync(mLogUrl + API_PATH, METHOD_POST, headers, callTemplate, serviceCallback);
    }

    @Override
    public void close() throws IOException {
        mHttpClient.close();
    }

    @Override
    public void reopen() {
        mHttpClient.reopen();
    }

    /**
     * Inner class is used to be able to mock System.currentTimeMillis, does not work if using anonymous inner class...
     */
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
            return mLogSerializer.serializeContainer(mLogContainer);
        }

        @Override
        public void onBeforeCalling(URL url, Map<String, String> headers) {
            if (AppCenterLog.getLogLevel() <= VERBOSE) {

                /* Log url. */
                AppCenterLog.verbose(LOG_TAG, "Calling " + url + "...");

                /* Log headers. */
                Map<String, String> logHeaders = new HashMap<>(headers);
                String appSecret = logHeaders.get(APP_SECRET);
                if (appSecret != null) {
                    logHeaders.put(APP_SECRET, HttpUtils.hideSecret(appSecret));
                }
                String authToken = logHeaders.get(AUTHORIZATION_HEADER);
                if (authToken != null) {
                    logHeaders.put(AUTHORIZATION_HEADER, HttpUtils.hideSecret(authToken));
                }
                AppCenterLog.verbose(LOG_TAG, "Headers: " + logHeaders);
            }
        }
    }
}
