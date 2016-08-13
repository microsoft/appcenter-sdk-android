package avalanche.core.ingestion.http;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCall;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.utils.AvalancheLog;

import static java.lang.Math.max;

public class AvalancheIngestionHttp implements AvalancheIngestion {

    /**
     * Default base URL.
     */
    private static final String DEFAULT_BASE_URL = "http://in-staging.avalanch.es:8081";

    /**
     * API Path.
     */
    private static final String API_PATH = "/logs?api-version=1.0.0-preview20160708";

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
     * Log tag for POST payload.
     */
    private static final String LOG_TAG = "AvalancheHttp";

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
     * Url connection factory.
     */
    private final UrlConnectionFactory mUrlConnectionFactory;

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
     * @param urlConnectionFactory url connection factory.
     * @param logSerializer        log serializer.
     */
    public AvalancheIngestionHttp(@NonNull UrlConnectionFactory urlConnectionFactory, @NonNull LogSerializer logSerializer) {
        mUrlConnectionFactory = urlConnectionFactory;
        mLogSerializer = logSerializer;
        mBaseUrl = DEFAULT_BASE_URL;
    }

    /**
     * Set the base url.
     *
     * @param baseUrl the base url.
     */
    public void setBaseUrl(@NonNull String baseUrl) {
        mBaseUrl = baseUrl;
    }

    @Override
    public ServiceCall sendAsync(final UUID appSecret, final UUID installId, final LogContainer logContainer, final ServiceCallback serviceCallback) throws IllegalArgumentException {
        final AsyncTask<Void, Void, Exception> call = new AsyncTask<Void, Void, Exception>() {

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    doCall(appSecret, installId, logContainer);
                } catch (Exception e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e == null)
                    serviceCallback.onCallSucceeded();
                else
                    serviceCallback.onCallFailed(e);
            }
        };
        call.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return new ServiceCall() {

            @Override
            public void cancel() {
                if (!call.isCancelled())
                    call.cancel(true);
            }
        };
    }

    /**
     * Do the HTTP call now.
     *
     * @param appSecret    a unique and secret key used to identify the application.
     * @param installId    install identifier.
     * @param logContainer payload.
     * @throws Exception if an error occurs.
     */
    private void doCall(UUID appSecret, UUID installId, LogContainer logContainer) throws Exception {

        /* HTTP session. */
        URL url = new URL(mBaseUrl + API_PATH);
        AvalancheLog.verbose(LOG_TAG, "Calling " + url + " ...");
        HttpURLConnection urlConnection = mUrlConnectionFactory.openConnection(url);
        try {

            /* Configure connection timeouts. */
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);

            /* Set headers. */
            urlConnection.setRequestProperty(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);
            urlConnection.setRequestProperty(APP_SECRET, appSecret.toString());
            urlConnection.setRequestProperty(INSTALL_ID, installId.toString());
            AvalancheLog.verbose(LOG_TAG, "Headers: " + urlConnection.getRequestProperties());

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
                payload = mLogSerializer.serializeContainer(logContainer);
            } finally {

                /* Restore original times, could be retried later. */
                for (int i = 0; i < size; i++)
                    logs.get(i).setToffset(absoluteTimes[i]);
            }
            AvalancheLog.verbose(LOG_TAG, payload);

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
            AvalancheLog.verbose(LOG_TAG, "HTTP response status=" + status + " payload=" + response);

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
    private String dump(HttpURLConnection urlConnection) throws IOException {

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

    @Override
    public void close() throws IOException {

        /* No-op. A decorator can take care of tracking calls to cancel. */
    }
}
