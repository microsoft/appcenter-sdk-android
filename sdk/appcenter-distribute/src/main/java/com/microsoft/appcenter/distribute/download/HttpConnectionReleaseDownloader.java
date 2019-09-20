package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.net.TrafficStats;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.distribute.BuildConfig;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.http.TLS1_2SocketFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.appcenter.distribute.BuildConfig.SDK_NAME;

public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private Context mContext;
    private Listener mListener;
    private static final int MAX_REDIRECTS = 6;
    private static final int TIMEOUT = 60000;
    private static final String SDK_USER_AGENT = "AppCenter/Android " + BuildConfig.VERSION_NAME;
    private static final int THREAD_STATS_TAG = SDK_NAME.hashCode();
    private static final String PREFERENCE_KEY_DOWNLOADING_FILE = "PREFERENCE_KEY_DOWNLOADING_FILE";
    private String mFilename;
    private File mDirectory;

    HttpConnectionReleaseDownloader(Context context) {
        mContext = context;
        mFilename = UUID.randomUUID() + ".apk";
        mDirectory = new File(context.getExternalFilesDir(null), "Download");
    }

    @Override
    public void download(ReleaseDetails releaseDetails) {
        InputStream input = null;
        OutputStream output = null;
        try {
            URL url = new URL(releaseDetails.getDownloadUrl().toString());
            TrafficStats.setThreadStatsTag(THREAD_STATS_TAG);
            URLConnection connection = createConnection(url, MAX_REDIRECTS);
            connection.connect();
            int lengthOfFile = connection.getContentLength();
            String contentType = connection.getContentType();
            if (contentType != null && contentType.contains("text")) {

                // This is not the expected APK file. Maybe the redirect could not be resolved.
                if (mListener != null) {
                    mListener.onError("The requested download does not appear to be a file.");
                }
                return;
            }
            boolean result = mDirectory.mkdirs();
            if (!result && !mDirectory.exists()) {
                throw new IOException("Could not create the dir(s):" + mDirectory.getAbsolutePath());
            }

            // Download the release file.
            File file = new File(mDirectory, this.mFilename);
            input = new BufferedInputStream(connection.getInputStream());
            output = new FileOutputStream(file);
            byte data[] = new byte[1024];
            int count;
            long total = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (mListener != null)
                    mListener.onProgress(Math.round(total * 100.0f / lengthOfFile), total);
                output.write(data, 0, count);
            }
            output.flush();
            if (mListener != null) {
                SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADING_FILE, file.getAbsolutePath());
                mListener.onComplete(file.getAbsolutePath());
            }
        } catch (IOException e) {
            AppCenterLog.error("Failed to download " + releaseDetails.getDownloadUrl().toString(), e.getMessage());
            return;
        } finally {
            TrafficStats.clearThreadStatsTag();
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

    @Override
    public void delete() {
        String localFilePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADING_FILE);
        if(localFilePath == null){
            return;
        }
        mContext.deleteFile(localFilePath);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADING_FILE);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }


    /**
     * Recursive method for resolving redirects. Resolves at most MAX_REDIRECTS times.
     *
     * @param url                a URL
     * @param remainingRedirects loop counter
     * @return instance of URLConnection
     * @throws IOException if connection fails
     */
    private URLConnection createConnection(URL url, int remainingRedirects) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(new TLS1_2SocketFactory());
        connection.addRequestProperty("User-Agent", SDK_USER_AGENT);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        int code = connection.getResponseCode();
        if (code == HttpsURLConnection.HTTP_MOVED_PERM ||
                code == HttpsURLConnection.HTTP_MOVED_TEMP ||
                code == HttpsURLConnection.HTTP_SEE_OTHER) {
            if (remainingRedirects == 0) {

                // Stop redirecting.
                return connection;
            }
            URL movedUrl = new URL(connection.getHeaderField("Location"));
            if (!url.getProtocol().equals(movedUrl.getProtocol())) {

                // HttpsURLConnection doesn't handle redirects across schemes, so handle it manually, see
                // http://code.google.com/p/android/issues/detail?id=41651
                connection.disconnect();
                return createConnection(movedUrl, --remainingRedirects); // Recursion
            }
        }
        return connection;
    }
}
