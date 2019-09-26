/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;

import com.microsoft.appcenter.http.TLS1_2SocketFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.appcenter.http.HttpUtils.CONNECT_TIMEOUT;
import static com.microsoft.appcenter.http.HttpUtils.READ_TIMEOUT;
import static com.microsoft.appcenter.http.HttpUtils.THREAD_STATS_TAG;
import static com.microsoft.appcenter.http.HttpUtils.WRITE_BUFFER_SIZE;

/**
 * Internal helper class. Downloads an .apk from AppCenter and stores
 * it on external storage. If the download was successful, the file
 * is then opened to trigger the installation.
 **/
class HttpDownloadFileTask extends AsyncTask<Void, Void, Void> {

    /**
     * Maximal number of allowed redirects.
     */
    private static final int MAX_REDIRECTS = 6;

    private static final int UPDATE_PROGRESS_BYTES_COUNT = 128 * 1024;

    private final WeakReference<HttpConnectionReleaseDownloader> mDownloader;

    private Uri mDownloadUri;

    /**
     * Path to the downloading apk.
     */
    private File mTargetFile;

    HttpDownloadFileTask(HttpConnectionReleaseDownloader downloader, Uri downloadUri, File targetFile) {
        mDownloader = new WeakReference<>(downloader);
        mDownloadUri = downloadUri;
        mTargetFile = targetFile;
    }

    @Override
    protected Void doInBackground(Void... args) {
        try {
            File directory = mTargetFile.getParentFile();
            if (directory == null || !(directory.exists() || directory.mkdirs())) {
                throw new IOException("Could not create the directory for file:" + mTargetFile.getAbsolutePath());
            }
            if (mTargetFile.exists()) {

                //noinspection ResultOfMethodCallIgnored
                mTargetFile.delete();
            }

            /* Create connection */
            URL url = new URL(mDownloadUri.toString());
            TrafficStats.setThreadStatsTag(THREAD_STATS_TAG);
            URLConnection connection = createConnection(url, MAX_REDIRECTS);
            connection.connect();
            String contentType = connection.getContentType();
            if (contentType != null && contentType.contains("text")) {

                /* This is not the expected APK file. Maybe the redirect could not be resolved. */
                throw new IOException("The requested download does not appear to be a file.");
            }

            /* Download the release file. */
            long totalBytesDownloaded = downloadFile(connection.getInputStream(), connection.getContentLength());
            if (totalBytesDownloaded > 0) {
                HttpConnectionReleaseDownloader downloader = mDownloader.get();
                if (downloader != null) {
                    downloader.onDownloadComplete(mTargetFile);
                }
            }
        } catch (IOException e) {
            HttpConnectionReleaseDownloader downloader = mDownloader.get();
            if (downloader != null) {
                downloader.onDownloadError(e.getMessage());
            }
        } finally {
            TrafficStats.clearThreadStatsTag();
        }
        return null;
    }

    /**
     * Performs IO operation to download .apk file through HttpConnection.
     * Saves .apk file to the mApkFilePath.
     *
     * @param inputStream  TODO
     * @param lengthOfFile TODO
     * @return total number of downloaded bytes.
     * @throws IOException if connection fails
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private long downloadFile(InputStream inputStream, long lengthOfFile) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new BufferedInputStream(inputStream);
            output = new FileOutputStream(mTargetFile);
            byte[] data = new byte[WRITE_BUFFER_SIZE];
            int count;
            long totalBytesDownloaded = 0, reported = 0;
            while ((count = input.read(data)) != -1) {
                totalBytesDownloaded += count;
                output.write(data, 0, count);

                /* Update the progress. */
                if (totalBytesDownloaded >= reported + UPDATE_PROGRESS_BYTES_COUNT || totalBytesDownloaded == lengthOfFile) {
                    HttpConnectionReleaseDownloader downloader = mDownloader.get();
                    if (downloader == null) {
                        break;
                    }
                    downloader.onDownloadProgress(totalBytesDownloaded, lengthOfFile);
                    reported += UPDATE_PROGRESS_BYTES_COUNT;
                }
                if (isCancelled()) {
                    break;
                }
            }
            output.flush();
            return totalBytesDownloaded;
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
     * Recursive method for resolving redirects. Resolves at most MAX_REDIRECTS times.
     *
     * @param url                a URL
     * @param remainingRedirects loop counter
     * @return instance of URLConnection
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
