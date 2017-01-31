package com.microsoft.azure.mobile.updates;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.DefaultHttpClient;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.HttpClientRetryer;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.azure.mobile.http.DefaultHttpClient.METHOD_GET;

public class Updates extends AbstractMobileCenterService {

    static final String EXTRA_UPDATE_TOKEN = "update_token";
    private static final String SERVICE_NAME = "Updates";
    static final String LOG_TAG = MobileCenter.LOG_TAG + SERVICE_NAME;
    private static final String GOOGLE_CHROME_URL_SCHEME = "googlechrome://navigate?url=";
    /**
     * TODO change to https once we have a real server.
     */
    private static final String GENERIC_BROWSER_URL_SCHEME = "http://";
    private static final String DEFAULT_LOGIN_PAGE_URL = "10.123.212.163:8080";
    private static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    private static final String PREFERENCE_KEY_UPDATE_TOKEN = PREFERENCE_PREFIX + EXTRA_UPDATE_TOKEN;

    private static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    private static final String CHECK_UPDATE_SERVER_URL = "http://10.123.212.163:8080/apps/%s/releases/latest";

    private static final String HEADER_UPDATE_TOKEN = "x-update-token";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Updates sInstance = null;

    private Context mContext;

    private String mAppSecret;

    private Activity mForegroundActivity;

    private boolean mLoginChecked;

    private HttpClient mHttpClient;

    private AsyncTask<Void, Void, Void> mCheckReleaseTask;

    private ServiceCall mCheckReleaseApiCall;

    private Object mCheckReleaseCallId;

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Updates getInstance() {
        if (sInstance == null) {
            sInstance = new Updates();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    @Override
    protected String getGroupName() {
        return null;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        super.onStarted(context, appSecret, channel);
        mContext = context;
        mAppSecret = appSecret;
        checkAndFetchUpdateToken();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mForegroundActivity = activity;
        checkAndFetchUpdateToken();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mForegroundActivity = null;
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        if (enabled) {
            checkAndFetchUpdateToken();
        } else {
            if (mCheckReleaseApiCall != null) {
                mCheckReleaseApiCall.cancel();
                mCheckReleaseApiCall = null;
                mCheckReleaseCallId = null;
            }
            if (mCheckReleaseTask != null) {
                mCheckReleaseTask.cancel(true);
                mCheckReleaseTask = null;
            }
            mLoginChecked = false;
            StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
            StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        }
    }

    private void checkAndFetchUpdateToken() {
        if (mForegroundActivity != null && !mLoginChecked && mCheckReleaseCallId == null) {

            String updateToken = StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN);
            if (updateToken != null && mCheckReleaseCallId == null) {
                checkUpdate(updateToken);
                return;
            }

            String baseUrl = DEFAULT_LOGIN_PAGE_URL + "?package=" + mForegroundActivity.getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW);

            /* Try to force using Chrome first, we want fall back url support for intent. */
            try {
                intent.setData(Uri.parse(GOOGLE_CHROME_URL_SCHEME + baseUrl));
                mForegroundActivity.startActivity(intent);
            } catch (ActivityNotFoundException e) {

                /* Fall back using a browser but we don't want a chooser U.I. to pop. */
                MobileCenterLog.debug(LOG_TAG, "Google Chrome not found, pick another one.");
                intent.setData(Uri.parse(GENERIC_BROWSER_URL_SCHEME + baseUrl));
                List<ResolveInfo> browsers = mForegroundActivity.getPackageManager().queryIntentActivities(intent, 0);
                if (browsers.isEmpty()) {
                    MobileCenterLog.error(LOG_TAG, "No browser found on device, abort login.");
                } else {

                    /*
                     * Check the default browser is not the picker,
                     * last thing we want is app to start and suddenly asks user to pick
                     * between 2 browsers without explaining why.
                     */
                    String defaultBrowserPackageName = null;
                    String defaultBrowserClassName = null;
                    ResolveInfo defaultBrowser = mForegroundActivity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (defaultBrowser != null) {
                        ActivityInfo activityInfo = defaultBrowser.activityInfo;
                        defaultBrowserPackageName = activityInfo.packageName;
                        defaultBrowserClassName = activityInfo.name;
                        MobileCenterLog.debug(LOG_TAG, "Default browser seems to be " + defaultBrowserPackageName + "/" + defaultBrowserClassName);
                    }
                    String selectedPackageName = null;
                    String selectedClassName = null;
                    for (ResolveInfo browser : browsers) {
                        ActivityInfo activityInfo = browser.activityInfo;
                        if (activityInfo.packageName.equals(defaultBrowserPackageName) && activityInfo.name.equals(defaultBrowserClassName)) {
                            selectedPackageName = defaultBrowserPackageName;
                            selectedClassName = defaultBrowserClassName;
                            MobileCenterLog.debug(LOG_TAG, "And its not the picker.");
                            break;
                        }
                    }
                    if (defaultBrowser != null && selectedPackageName == null) {
                        MobileCenterLog.debug(LOG_TAG, "Default browser is actually a picker...");
                    }

                    /* If no default browser found, pick first one we can find. */
                    if (selectedPackageName == null) {
                        MobileCenterLog.debug(LOG_TAG, "Picking first browser in list.");
                        ResolveInfo browser = browsers.iterator().next();
                        ActivityInfo activityInfo = browser.activityInfo;
                        selectedPackageName = activityInfo.packageName;
                        selectedClassName = activityInfo.name;
                    }

                    /* Launch generic browser. */
                    MobileCenterLog.debug(LOG_TAG, "Launch browser=" + selectedPackageName + "/" + selectedClassName);
                    intent.setClassName(selectedPackageName, selectedClassName);
                    mForegroundActivity.startActivity(intent);
                }
            }
            mLoginChecked = true;
        }
    }

    /*
     * Store update token and possibly trigger application update check.
     * TODO encrypt token, but where to store encryption key? If it's retrieved from server,
     * how do we protect server call to get the key in the first place?
     * Even having the encryption key temporarily in memory is risky as that can be heap dumped.
     */
    synchronized void storeUpdateToken(@NonNull Context context, @NonNull String updateToken) {
        if (isInstanceEnabled()) {
            if (mChannel == null) {
                StorageHelper.initialize(context);
            }
            StorageHelper.PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, updateToken);
            if (mCheckReleaseCallId == null) {
                checkUpdate(updateToken);
            }
            // TODO else cancel request retry?
        }
    }

    private synchronized void checkUpdate(@NonNull String updateToken) {
        if (mHttpClient == null) {
            HttpClientRetryer retryer = new HttpClientRetryer(new DefaultHttpClient());
            NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(mContext);
            mHttpClient = new HttpClientNetworkStateHandler(retryer, networkStateHelper);
        }
        String url = String.format(CHECK_UPDATE_SERVER_URL, mAppSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_UPDATE_TOKEN, updateToken);
        final Object releaseCallId = mCheckReleaseCallId = new Object();
        mCheckReleaseApiCall = mHttpClient.callAsync(url, METHOD_GET, headers, null, new ServiceCallback() {

            @Override
            public void onCallSucceeded(String payload) {
                try {
                    compareVersions(releaseCallId, ReleaseDetails.parse(payload));
                } catch (JSONException e) {
                    onCallFailed(e);
                }
            }

            @Override
            public void onCallFailed(Exception e) {
                MobileCenterLog.error(LOG_TAG, "Failed to check latest release:", e);
            }
        });
    }

    /**
     * Query package manager and compute hash in background.
     */
    private synchronized void compareVersions(Object releaseCallId, final ReleaseDetails releaseDetails) {

        /* Check if state did not change. */
        if (mCheckReleaseCallId == releaseCallId && isInstanceEnabled()) {
            mCheckReleaseTask = new CheckReleaseDetails(releaseDetails).execute();
        }
    }

    /**
     * Persist download state.
     *
     * @param task              current task to check race conditions.
     * @param downloadRequestId download identifier.
     */
    private synchronized void storeDownloadRequestId(CheckReleaseDetails task, long downloadRequestId) {

        /* Check for if state changed and task not canceled in time. */
        if (mCheckReleaseTask == task && isInstanceEnabled()) {

            /* No state change, let download proceed and store state. */
            StorageHelper.PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadRequestId);
        } else {

            /* State changed quickly, cancel download. */
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
            downloadManager.remove(downloadRequestId);
        }
    }

    /**
     * Inspecting release details can take some time, especially if we have to compute a hash.
     */
    private class CheckReleaseDetails extends AsyncTask<Void, Void, Void> {

        /**
         * Release details to check.
         */
        private final ReleaseDetails mReleaseDetails;

        /**
         * Init.
         *
         * @param releaseDetails release details associated to this check.
         */
        CheckReleaseDetails(ReleaseDetails releaseDetails) {
            this.mReleaseDetails = releaseDetails;
        }

        @Override
        protected Void doInBackground(Void[] params) {

            /* TODO Check minimum API level, there is a spec problem currently on that on JSON. */

            /* Check version code. */
            boolean isMoreRecent = false;
            PackageManager packageManager = mContext.getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
                if (mReleaseDetails.getVersion() > packageInfo.versionCode) {
                    isMoreRecent = true;
                } else if (mReleaseDetails.getVersion() == packageInfo.versionCode) {
                    // FIXME check hash when version code is same
                    isMoreRecent = false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                MobileCenterLog.error(LOG_TAG, "Could not compare versions.", e);
                return null;
            }

            /* Start download if build considered more recent. */
            if (isMoreRecent) {
                DownloadManager.Request request = new DownloadManager.Request(mReleaseDetails.getDownloadUrl());
                request.setMimeType("application/vnd.android.package-archive"); // FIXME useless, see below

                /*
                 * TODO you can't have notification click working that way, it will fail to open file.
                 * We need anyway to listen for completion, upon completion either:
                 * - If we are in foreground, pop install UI ourselves.
                 * - If we are in background, place a notification in panel (that just resumes application).
                 * When clicking on it: launch install UI.
                 * Also pop install U.I. if application resumes if user did not action notification.
                 */
                request.setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
                long downloadRequestId = downloadManager.enqueue(request);
                storeDownloadRequestId(this, downloadRequestId);
            }
            return null;
        }
    }
}
