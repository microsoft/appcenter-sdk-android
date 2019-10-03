/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.http.TLS1_2SocketFactory;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_PROGRESS_BYTES_THRESHOLD;
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_PROGRESS_TIME_THRESHOLD;
import static com.microsoft.appcenter.http.HttpUtils.CONNECT_TIMEOUT;
import static com.microsoft.appcenter.http.HttpUtils.READ_TIMEOUT;
import static com.microsoft.appcenter.http.HttpUtils.THREAD_STATS_TAG;
import static com.microsoft.appcenter.http.HttpUtils.WRITE_BUFFER_SIZE;

/**
 * Downloads an update and stores it on specified file.
 **/
class HttpConnectionDownloadFileTask extends AsyncTask<Void, Void, Void> {

    /**
     * Maximal number of allowed redirects.
     */
    private static final int MAX_REDIRECTS = 6;

    @VisibleForTesting
    static final String APK_CONTENT_TYPE = "application/vnd.android.package-archive";

    private final HttpConnectionReleaseDownloader mDownloader;

    /**
     * The URI that hosts the binary to download.
     */
    private final Uri mDownloadUri;

    /**
     * The file that used to write downloaded package.
     */
    private final File mTargetFile;

    HttpConnectionDownloadFileTask(HttpConnectionReleaseDownloader downloader, Uri downloadUri, File targetFile) {
        mDownloader = downloader;
        mDownloadUri = downloadUri;
        mTargetFile = targetFile;
    }

    @Override
    protected Void doInBackground(Void... args) {
        try {
            long enqueueTime = System.currentTimeMillis();
            mDownloader.onDownloadStarted(enqueueTime);

            /* Create connection. */
            URL url = new URL(mDownloadUri.toString());
            TrafficStats.setThreadStatsTag(THREAD_STATS_TAG);
            URLConnection connection = createConnection(url, MAX_REDIRECTS);
            connection.connect();

            /* Content type check. Produce only warning if it doesn't match. */
            String contentType = connection.getContentType();
            if (!APK_CONTENT_TYPE.equals(contentType)) {
                AppCenterLog.warn(LOG_TAG, "The requested download has not expected content type.");
            }

            /* Download the release file. */
            long totalBytesDownloaded = downloadFile(connection);
            if (totalBytesDownloaded > 0) {
                mDownloader.onDownloadComplete(mTargetFile);
            }
        } catch (IOException e) {
            mDownloader.onDownloadError(e.getMessage());
        } finally {
            TrafficStats.clearThreadStatsTag();
        }
        return null;
    }

    /**
     * Performs IO operation to download file through {@link URLConnection}.
     * Saves the file to the {@link #mTargetFile).
     *
     * @param connection network connection,
     * @return total number of downloaded bytes.
     * @throws IOException if connection fails.
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private long downloadFile(URLConnection connection) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new BufferedInputStream(connection.getInputStream());
            output = new FileOutputStream(mTargetFile);
            return copyStream(input, output, connection.getContentLength());
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Copies one stream into another and reports the progress.
     *
     * @param inputStream  the input stream.
     * @param outputStream the output stream.
     * @param lengthOfFile total number of bytes in the input stream. Used only for progress reporting.
     * @return total number of processed bytes.
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    private long copyStream(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, long lengthOfFile) throws IOException {
        byte[] data = new byte[WRITE_BUFFER_SIZE];
        int count;
        long totalBytesDownloaded = 0;
        long lastReportedBytes = 0;
        long lastReportedTime = 0;
        while ((count = inputStream.read(data)) != -1) {
            totalBytesDownloaded += count;
            outputStream.write(data, 0, count);

            /* Update the progress each UPDATE_PROGRESS_BYTES_COUNT bytes. */
            long now = System.currentTimeMillis();
            if (totalBytesDownloaded >= lastReportedBytes + UPDATE_PROGRESS_BYTES_THRESHOLD || totalBytesDownloaded == lengthOfFile ||
                    now >= lastReportedTime + UPDATE_PROGRESS_TIME_THRESHOLD) {
                mDownloader.onDownloadProgress(totalBytesDownloaded, lengthOfFile);
                lastReportedBytes = totalBytesDownloaded;
                lastReportedTime = now;
            }

            /* Check if the task is cancelled. */
            if (isCancelled()) {
                break;
            }
        }
        outputStream.flush();
        return totalBytesDownloaded;
    }

    /**
     * Recursive method for resolving redirects. Resolves at most MAX_REDIRECTS times.
     *
     * @param url                a URL.
     * @param remainingRedirects redirects counter.
     * @return instance of {@link URLConnection}.
     * @throws IOException if connection fails
     */
    private static URLConnection createConnection(URL url, int remainingRedirects) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(new TLS1_2SocketFactory());
        connection.setInstanceFollowRedirects(true);

        /* Configure connection timeouts. */
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        /* Check redirects. */
        int code = connection.getResponseCode();
        if (code == HttpsURLConnection.HTTP_MOVED_PERM ||
                code == HttpsURLConnection.HTTP_MOVED_TEMP ||
                code == HttpsURLConnection.HTTP_SEE_OTHER) {
            if (remainingRedirects == 0) {

                /* Stop redirecting. */
                return connection;
            }
            URL movedUrl = new URL(connection.getHeaderField("Location"));
            if (!url.getProtocol().equals(movedUrl.getProtocol())) {

                /*
                 * HttpsURLConnection doesn't handle redirects across schemes, so handle it manually,
                 * see http://code.google.com/p/android/issues/detail?id=41651
                 */
                connection.disconnect();
                return createConnection(movedUrl, --remainingRedirects);
            }
        }
        return connection;
    }
}
