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
import static com.microsoft.appcenter.http.HttpUtils.THREAD_STATS_TAG;
import static com.microsoft.appcenter.http.HttpUtils.WRITE_BUFFER_SIZE;
import static com.microsoft.appcenter.http.HttpUtils.createHttpsConnection;

/**
 * Downloads an update and stores it on specified file.
 **/
class HttpConnectionDownloadFileTask extends AsyncTask<Void, Void, Void> {

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
    protected Void doInBackground(Void... params) {

        /* Do tag socket to avoid strict mode issue. */
        TrafficStats.setThreadStatsTag(THREAD_STATS_TAG);
        try {
            long enqueueTime = System.currentTimeMillis();
            mDownloader.onDownloadStarted(enqueueTime);

            /* Create connection. */
            URLConnection connection = createConnection();

            /* Download the release file. */
            long totalBytesDownloaded = downloadFile(connection);
            if (totalBytesDownloaded > 0) {
                mDownloader.onDownloadComplete(mTargetFile);
            } else {
                throw new IOException("The content of downloaded file is empty");
            }
        } catch (IOException e) {
            mDownloader.onDownloadError(e.getMessage());
        } finally {
            TrafficStats.clearThreadStatsTag();
        }
        return null;
    }

    /**
     * Create connection for downloading.
     *
     * @return instance of {@link URLConnection}.
     * @throws IOException if connection fails.
     */
    private URLConnection createConnection() throws IOException {

        /* Create connection. */
        URL url = new URL(mDownloadUri.toString());
        HttpsURLConnection connection = createHttpsConnection(url);
        connection.setInstanceFollowRedirects(true);
        connection.connect();

        /* Content type check. Produce only warning if it doesn't match. */
        String contentType = connection.getContentType();
        if (!APK_CONTENT_TYPE.equals(contentType)) {
            AppCenterLog.warn(LOG_TAG, "The requested download has not expected content type.");
        }

        /* Accept all 2xx codes. */
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Download failed with HTTP error code: " + responseCode);
        }
        return connection;
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
}
