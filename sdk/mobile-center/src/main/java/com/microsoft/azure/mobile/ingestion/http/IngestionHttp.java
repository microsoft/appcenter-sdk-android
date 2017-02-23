package com.microsoft.azure.mobile.ingestion.http;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.ingestion.Ingestion;
import com.microsoft.azure.mobile.ingestion.ServiceCall;
import com.microsoft.azure.mobile.ingestion.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static android.util.Log.VERBOSE;
import static com.microsoft.azure.mobile.MobileCenter.LOG_TAG;
import static java.lang.Math.max;

public class IngestionHttp implements Ingestion {

    /**
     * Default base URL.
     */
    private static final String DEFAULT_BASE_URL = "https://in.mobile.azure.com";

    /**
     * API Path.
     */
    private static final String API_PATH = "/logs?api_version=1.0.0-preview20160914";

    /**
     * Content type header value.
     */
    private static final String CONTENT_TYPE_VALUE = "application/json";

    /**
     * Application secret HTTP Header.
     */
    private static final String APP_SECRET = "App-Secret";

    /**
     * Installation identifier HTTP Header.
     */
    private static final String INSTALL_ID = "Install-ID";

    /**
     * Default string builder capacity.
     */
    private static final int DEFAULT_STRING_BUILDER_CAPACITY = 16;

    /**
     * Content type header key.
     */
    private static final String CONTENT_TYPE_KEY = "Content-Type";

    /**
     * Character encoding.
     */
    private static final String CHARSET_NAME = "UTF-8";

    /**
     * Read buffer size.
     */
    private static final int READ_BUFFER_SIZE = 1024;

    /**
     * HTTP connection timeout.
     */
    private static final int CONNECT_TIMEOUT = 60000;

    /**
     * HTTP read timeout.
     */
    private static final int READ_TIMEOUT = 20000;

    /**
     * Maximum characters to be displayed in a log for application secret.
     */
    private static final int MAX_CHARACTERS_DISPLAYED_FOR_APP_SECRET = 8;

    /**
     * Log serializer.
     */
    private final LogSerializer mLogSerializer;

    /**
     * API base URL (scheme + authority).
     */
    private String mBaseUrl;

    /**
     * Init.
     *
     * @param logSerializer log serializer.
     */
    public IngestionHttp(@NonNull LogSerializer logSerializer) {
        mLogSerializer = logSerializer;
        mBaseUrl = DEFAULT_BASE_URL;
    }

    /**
     * Do the HTTP call now.
     *
     * @param baseUrl       API base URL (scheme + authority).
     * @param logSerializer log serializer.
     * @param appSecret     a unique and secret key used to identify the application.
     * @param installId     install identifier.
     * @param logContainer  payload.    @throws Exception if an error occurs.
     */
    private static void doCall(String baseUrl, LogSerializer logSerializer, String appSecret, UUID installId, LogContainer logContainer) throws Exception {

        /* HTTP session. */
        URL url = new URL(baseUrl + API_PATH);
        MobileCenterLog.verbose(LOG_TAG, "Calling " + url + " ...");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {

            /* Configure connection timeouts. */
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);

            /* Set headers. */
            urlConnection.setRequestProperty(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);
            urlConnection.setRequestProperty(APP_SECRET, appSecret);
            urlConnection.setRequestProperty(INSTALL_ID, installId.toString());

            /* Log headers. */
            if (MobileCenterLog.getLogLevel() <= VERBOSE) {
                int hidingEndIndex = appSecret.length() - (appSecret.length() >= MAX_CHARACTERS_DISPLAYED_FOR_APP_SECRET ? MAX_CHARACTERS_DISPLAYED_FOR_APP_SECRET : 0);
                char[] fill = new char[hidingEndIndex];
                Arrays.fill(fill, '*');
                String header = "Headers: " + CONTENT_TYPE_KEY + '=' + CONTENT_TYPE_VALUE +
                        ", " + APP_SECRET + '=' + new String(fill) + appSecret.substring(hidingEndIndex) +
                        ", " + INSTALL_ID + '=' + installId.toString();
                MobileCenterLog.verbose(LOG_TAG, header);
            }

            /* Timestamps need to be as accurate as possible so we convert absolute time to relative now. Save times. */
            List<Log> logs = logContainer.getLogs();
            int size = logs.size();
            long[] absoluteTimes = new long[size];
            for (int i = 0; i < size; i++) {
                Log log = logs.get(i);
                long toffset = log.getToffset();
                absoluteTimes[i] = toffset;
                log.setToffset(System.currentTimeMillis() - toffset);
            }

            /* Serialize payload. */
            String payload;
            try {
                payload = logSerializer.serializeContainer(logContainer);
            } finally {

                /* Restore original times, could be retried later. */
                for (int i = 0; i < size; i++)
                    logs.get(i).setToffset(absoluteTimes[i]);
            }
            MobileCenterLog.verbose(LOG_TAG, payload);

            /* Send payload through the wire. */
            byte[] binaryPayload = payload.getBytes(CHARSET_NAME);
            urlConnection.setDoOutput(true);
            urlConnection.setFixedLengthStreamingMode(binaryPayload.length);
            OutputStream out = urlConnection.getOutputStream();
            out.write(binaryPayload);
            out.close();

            /* Read response. */
            int status = urlConnection.getResponseCode();
            String response = dump(urlConnection);
            MobileCenterLog.verbose(LOG_TAG, "HTTP response status=" + status + " payload=" + response);

            /* Generate exception on failure. */
            if (status != 200)
                throw new HttpException(status, response);
        } finally {

            /* Release connection. */
            urlConnection.disconnect();
        }
    }

    /**
     * Dump stream to string.
     *
     * @param urlConnection URL connection.
     * @return dumped string.
     * @throws IOException if an error occurred.
     */
    private static String dump(HttpURLConnection urlConnection) throws IOException {

        /*
         * Though content length header value is less than actual payload length (gzip), we want to init
         * buffer with a reasonable start size to optimize (default is 16 and is way too low for this
         * use case).
         */
        StringBuilder builder = new StringBuilder(max(urlConnection.getContentLength(), DEFAULT_STRING_BUILDER_CAPACITY));
        InputStream stream;
        if (urlConnection.getResponseCode() < 400)
            stream = urlConnection.getInputStream();
        else
            stream = urlConnection.getErrorStream();
        InputStreamReader in = new InputStreamReader(stream, CHARSET_NAME);
        char[] buffer = new char[READ_BUFFER_SIZE];
        int len;
        while ((len = in.read(buffer)) > 0)
            builder.append(buffer, 0, len);
        return builder.toString();
    }

    /**
     * Set the base URL.
     *
     * @param logUrl the base URL.
     */
    @Override
    @SuppressWarnings("SameParameterValue")
    public void setLogUrl(@NonNull String logUrl) {
        mBaseUrl = logUrl;
    }

    @Override
    public ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, final ServiceCallback serviceCallback) throws IllegalArgumentException {
        final Call call = new Call(mBaseUrl, mLogSerializer, appSecret, installId, logContainer, serviceCallback);
        try {
            call.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (final RejectedExecutionException e) {

            /*
             * When executor saturated (shared with app), we should use the retry mechanism
             * rather than creating more threads to avoid putting too much pressure on the hosting app.
             * Also we need to return the method before calling the listener,
             * so we post the callback on handler to make sure of that.
             */
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    serviceCallback.onCallFailed(e);
                }
            });
        }
        return new ServiceCall() {

            @Override
            public void cancel() {
                if (!call.isCancelled())
                    call.cancel(true);
            }
        };
    }

    @Override
    public void close() throws IOException {

        /* No-op. A decorator can take care of tracking calls to cancel. */
    }

    @VisibleForTesting
    static class Call extends AsyncTask<Void, Void, Exception> {

        private final String mBaseUrl;

        private final LogSerializer mLogSerializer;

        private final String mAppSecret;

        private final UUID mInstallId;

        private final LogContainer mLogContainer;

        private final ServiceCallback mServiceCallback;

        Call(String baseUrl, LogSerializer logSerializer, String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
            mBaseUrl = baseUrl;
            mLogSerializer = logSerializer;
            mAppSecret = appSecret;
            mInstallId = installId;
            mLogContainer = logContainer;
            mServiceCallback = serviceCallback;
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                doCall(mBaseUrl, mLogSerializer, mAppSecret, mInstallId, mLogContainer);
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null)
                mServiceCallback.onCallSucceeded();
            else
                mServiceCallback.onCallFailed(e);
        }
    }
}
