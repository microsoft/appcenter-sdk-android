package com.microsoft.appcenter.distribute.download;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.TrafficStats;
import android.os.AsyncTask;

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
import static com.microsoft.appcenter.distribute.download.ReleaseDownloader.Listener;

/**
 * <h3>Description</h3>
 * <p>
 * Internal helper class. Downloads an .apk from AppCenter and stores
 * it on external storage. If the download was successful, the file
 * is then opened to trigger the installation.
 **/
@SuppressLint("StaticFieldLeak")
public class DownloadFileTask extends AsyncTask<Void, Integer, Long> {
    private static final int MAX_REDIRECTS = 6;
    private static final int TIMEOUT = 60000;
    private static final int THREAD_STATS_TAG = SDK_NAME.hashCode();
    private static final String PREFERENCE_KEY_DOWNLOADING_FILE = "PREFERENCE_KEY_DOWNLOADING_FILE";
    private static final String SDK_USER_AGENT = "AppCenter/Android " + BuildConfig.VERSION_NAME;
    private Context mContext;
    private Listener mListener;
    private File mApkFilePath;
    private File mDirectory;
    private ReleaseDetails mReleaseDetails;

    DownloadFileTask(Context context, ReleaseDetails releaseDetails) {
        mContext = context;
        mReleaseDetails = releaseDetails;
        mDirectory = new File(context.getExternalFilesDir(null), "Download");
        mApkFilePath = resolveApkFilePath(context);
    }

    @Override
    protected Long doInBackground(Void... args) {
        InputStream input = null;
        OutputStream output = null;
        try {
            URL url = new URL(mReleaseDetails.getDownloadUrl().toString());
            TrafficStats.setThreadStatsTag(THREAD_STATS_TAG);
            URLConnection connection = createConnection(url, MAX_REDIRECTS);
            connection.connect();
            int lengthOfFile = connection.getContentLength();
            String contentType = connection.getContentType();
            if (contentType != null && contentType.contains("text")) {

                /* This is not the expected APK file. Maybe the redirect could not be resolved. */
                if (mListener != null) {
                    mListener.onError("The requested download does not appear to be a file.");
                }
                return 0L;
            }
            boolean result = mDirectory.mkdirs();
            if (!result && !mDirectory.exists()) {
                throw new IOException("Could not create the dir(s):" + mDirectory.getAbsolutePath());
            }

            /* Download the release file. */
            input = new BufferedInputStream(connection.getInputStream());
            output = new FileOutputStream(mApkFilePath);
            byte data[] = new byte[1024];
            int count;
            long total = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (mListener != null) {
                    mListener.onProgress(Math.round(total * 100.0f / lengthOfFile), total);
                }
                output.write(data, 0, count);
            }
            output.flush();
            return total;
        } catch (IOException e) {
            AppCenterLog.error("Failed to download " + mReleaseDetails.getDownloadUrl().toString(), e.getMessage());
            return 0L;
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
    protected void onPostExecute(Long result) {
        if (result > 0L && mListener != null) {
            SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADING_FILE, mApkFilePath.getAbsolutePath());
            mListener.onComplete(mApkFilePath.getAbsolutePath());
        }
    }

    void attachListener(Listener listener) {
        mListener = listener;
    }

    void detachListener() {
        mListener = null;
    }

    private File resolveApkFilePath(Context context){
        this.mDirectory = new File(context.getExternalFilesDir(null), "Download");
        return new File(mDirectory, UUID.randomUUID() + ".apk");
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

                /*  Stop redirecting. */
                return connection;
            }
            URL movedUrl = new URL(connection.getHeaderField("Location"));
            if (!url.getProtocol().equals(movedUrl.getProtocol())) {

                /*  HttpsURLConnection doesn't handle redirects across schemes, so handle it manually, see
                    http://code.google.com/p/android/issues/detail?id=41651 */
                connection.disconnect();
                return createConnection(movedUrl, --remainingRedirects); // Recursion
            }
        }
        return connection;
    }
 }
