package com.microsoft.azure.mobile.updates;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.AsyncTaskUtils;
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

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.azure.mobile.http.DefaultHttpClient.METHOD_GET;

/**
 * Updates service.
 */
public class Updates extends AbstractMobileCenterService {

    /**
     * Used for deep link intent from browser, string field for update token.
     */
    static final String EXTRA_UPDATE_TOKEN = "update_token";

    /**
     * Update service name.
     */
    private static final String SERVICE_NAME = "Updates";

    /**
     * Log tag for this service.
     */
    static final String LOG_TAG = MobileCenter.LOG_TAG + SERVICE_NAME;

    /**
     * Scheme used to open URLs in Google Chrome instead of any browser.
     */
    private static final String GOOGLE_CHROME_URL_SCHEME = "googlechrome://navigate?url=";

    /**
     * Scheme used to open URLs in any browser. TODO change to https once we have a real server.
     */
    private static final String GENERIC_BROWSER_URL_SCHEME = "http://";

    /**
     * URL without scheme to open browser to login.
     */
    private static final String DEFAULT_LOGIN_PAGE_URL = "10.123.212.163:8080";

    /**
     * Full URL to call server to check latest release.
     */
    private static final String CHECK_UPDATE_SERVER_URL = "http://10.123.212.163:8080/apps/%s/releases/latest";

    /**
     * Header used to pass token when checking latest release.
     */
    private static final String HEADER_API_TOKEN = "x-api-token";

    /**
     * Base key for stored preferences.
     */
    private static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store token.
     */
    private static final String PREFERENCE_KEY_UPDATE_TOKEN = PREFERENCE_PREFIX + EXTRA_UPDATE_TOKEN;

    /**
     * Preference key to store the last download identifier.
     */
    private static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Preference key to store the last download file location on download manager.
     */
    private static final String PREFERENCE_KEY_DOWNLOAD_URI = PREFERENCE_PREFIX + "download_uri";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Updates sInstance = null;

    /**
     * Application context, if not null it means onStart was called.
     */
    private Context mContext;

    /**
     * Application secret.
     */
    private String mAppSecret;

    /**
     * If not null we are in foreground inside this activity.
     */
    private Activity mForegroundActivity;

    /**
     * Remember if we already opened browser to login.
     */
    private boolean mBrowserOpened;

    /**
     * In memory token if we receive deep link intent before onStart.
     */
    private String mBeforeStartUpdateToken;

    /**
     * Current API call identifier to check latest release from server, used for state check.
     * We can't use the ServiceCall object for that purpose because of a chicken and egg problem.
     */
    private Object mCheckReleaseCallId;

    /**
     * Current API call to check latest release from server.
     */
    private ServiceCall mCheckReleaseApiCall;

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private AsyncTask<?, ?, ?> mInspectReleaseTask;

    /**
     * Current task to process download completion.
     */
    private AsyncTask<?, ?, ?> mProcessDownloadCompletionTask;

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

    /**
     * Check whether Updates service is enabled or not.
     *
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable Updates service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Get the intent used to open installation U.I.
     *
     * @param fileUri downloaded file URI from the download manager.
     * @return intent to open installation U.I.
     */
    @NonNull
    private static Intent getInstallIntent(Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /**
     * Get the notification identifier for downloads.
     *
     * @return notification identifier for downloads.
     */
    private static int getNotificationId() {
        return (Updates.class.getName()).hashCode();
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
        resumeUpdateWorkflow();
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mForegroundActivity = activity;
        resumeUpdateWorkflow();
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        mForegroundActivity = null;
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        if (enabled) {
            resumeUpdateWorkflow();
        } else {

            /* Clean all state on disabling, cancel everything. */
            mBrowserOpened = false;
            cancelPreviousTasks();
            StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        }
    }

    /**
     * Cancel everything.
     */
    private synchronized void cancelPreviousTasks() {
        if (mCheckReleaseApiCall != null) {
            mCheckReleaseApiCall.cancel();
            mCheckReleaseApiCall = null;
            mCheckReleaseCallId = null;
        }
        if (mInspectReleaseTask != null) {
            mInspectReleaseTask.cancel(true);
            mInspectReleaseTask = null;
        }
        if (mProcessDownloadCompletionTask != null) {
            mProcessDownloadCompletionTask.cancel(true);
            mProcessDownloadCompletionTask = null;
        }
        long downloadId = StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID);
        if (downloadId > 0) {
            MobileCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.remove(downloadId);
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(getNotificationId());
        }
        StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    /**
     * Method that triggers the update workflow or proceed to the next step.
     */
    private synchronized void resumeUpdateWorkflow() {
        if (mForegroundActivity != null) {

            /* If we received the update token before Mobile Center was started/enabled, process it now. */
            if (mBeforeStartUpdateToken != null) {
                MobileCenterLog.debug(LOG_TAG, "Processing update token we kept in memory before onStarted");
                storeUpdateToken(mBeforeStartUpdateToken);
                mBeforeStartUpdateToken = null;
                return;
            }

            /* If we have a download ready but we were in background, pop install UI now. */
            try {
                Uri apkUri = Uri.parse(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_DOWNLOAD_URI));
                StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
                MobileCenterLog.debug(LOG_TAG, "Now in foreground, remove notification and start install for APK uri=" + apkUri);
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(getNotificationId());
                mForegroundActivity.startActivity(getInstallIntent(apkUri));
                return;
            } catch (RuntimeException e) {
                MobileCenterLog.verbose(LOG_TAG, "No APK downloaded or user ignored it, proceed state check.");
            }

            /* Nothing more to do for now if we are already calling API to check release. */
            if (mCheckReleaseCallId != null) {
                MobileCenterLog.verbose(LOG_TAG, "Already checking or checked latest release.");
                return;
            }

            /* Check if we have previous stored the update token. */
            String updateToken = StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN);
            if (updateToken != null) {
                getLatestReleaseDetails(updateToken);
                return;
            }

            /* If not, open browser to login. */
            if (mBrowserOpened) {
                return;
            }
            MobileCenterLog.debug(LOG_TAG, "No token, need to open browser to login.");
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
            mBrowserOpened = true;
        }
    }

    /*
     * Store update token and possibly trigger application update check.
     * TODO encrypt token, but where to store encryption key? If it's retrieved from server,
     * how do we protect server call to get the key in the first place?
     * Even having the encryption key temporarily in memory is risky as that can be heap dumped.
     */
    synchronized void storeUpdateToken(@NonNull String updateToken) {

        /* Keep token for later if we are not started and enabled yet. */
        if (mContext == null) {
            MobileCenterLog.debug(LOG_TAG, "Update token received before onStart, keep it in memory.");
            mBeforeStartUpdateToken = updateToken;
        } else if (isInstanceEnabled()) {
            StorageHelper.PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, updateToken);
            MobileCenterLog.debug(LOG_TAG, "Stored update token.");
            cancelPreviousTasks();
            getLatestReleaseDetails(updateToken);
        }
    }

    /**
     * Get latest release details from server.
     *
     * @param updateToken token to secure API call.
     */
    private synchronized void getLatestReleaseDetails(@NonNull String updateToken) {
        MobileCenterLog.debug(LOG_TAG, "Get latest release details...");
        HttpClientRetryer retryer = new HttpClientRetryer(new DefaultHttpClient());
        NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(mContext);
        HttpClient httpClient = new HttpClientNetworkStateHandler(retryer, networkStateHelper);
        String url = String.format(CHECK_UPDATE_SERVER_URL, mAppSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_API_TOKEN, updateToken);
        final Object releaseCallId = mCheckReleaseCallId = new Object();
        mCheckReleaseApiCall = httpClient.callAsync(url, METHOD_GET, headers, null, new ServiceCallback() {

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
        if (mCheckReleaseCallId == releaseCallId) {
            MobileCenterLog.debug(LOG_TAG, "Schedule background version check...");
            mInspectReleaseTask = AsyncTaskUtils.execute(LOG_TAG, new CheckReleaseDetails(releaseDetails));
        }
    }

    /**
     * Persist download state.
     *
     * @param downloadManager   download manager.
     * @param task              current task to check race conditions.
     * @param downloadRequestId download identifier.
     */
    private synchronized void storeDownloadRequestId(DownloadManager downloadManager, CheckReleaseDetails task, long downloadRequestId) {

        /* Check for if state changed and task not canceled in time. */
        if (mInspectReleaseTask == task) {

            /* Delete previous download. */
            long previousDownloadId = StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID);
            if (previousDownloadId > 0) {
                MobileCenterLog.debug(LOG_TAG, "Delete previous download an notification id=" + previousDownloadId);
                downloadManager.remove(previousDownloadId);
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(getNotificationId());
            }

            /* Store new download identifier. */
            StorageHelper.PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadRequestId);
        } else {

            /* State changed quickly, cancel download. */
            MobileCenterLog.debug(LOG_TAG, "State changed while downloading, cancel id=" + downloadRequestId);
            downloadManager.remove(downloadRequestId);
        }
    }

    /**
     * Bring app to foreground if in background.
     *
     * @param context any application context.
     */
    synchronized void resumeApp(@NonNull Context context) {

        /* Nothing to do if already in foreground. */
        if (mForegroundActivity == null) {

            /*
             * Use our deep link activity with no parameter just to resume app correctly
             * without duplicating activities or clearing task.
             */
            Intent intent = new Intent(context, DeepLinkActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Check a download that just completed.
     *
     * @param context    any application context.
     * @param downloadId download identifier from DownloadManager.
     */
    synchronized void processCompletedDownload(@NonNull Context context, long downloadId) {

        /* Querying download manager and even the start intent violate strict mode so do that in background. */
        mProcessDownloadCompletionTask = AsyncTaskUtils.execute(LOG_TAG, new ProcessDownloadCompletion(context, downloadId));
    }

    /**
     * Used by task processing the download completion in background prior to showing install U.I to check if request was canceled.
     *
     * @param task task to check state for.
     * @return foreground activity if any, if state is valid.
     * @throws IllegalStateException if state changed.
     */
    private synchronized Activity checkStateIsValidFor(ProcessDownloadCompletion task) throws IllegalStateException {
        if (task == mProcessDownloadCompletionTask) {
            return mForegroundActivity;
        }
        throw new IllegalStateException();
    }

    /**
     * Post notification about a completed download if state did not change.
     *
     * @param context      context.
     * @param task         task that prepared the notification to check state.
     * @param notification notification to post.
     */
    private synchronized void notifyDownload(Context context, ProcessDownloadCompletion task, Notification notification) {
        if (task == mProcessDownloadCompletionTask) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(getNotificationId(), notification);
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
            mReleaseDetails = releaseDetails;
        }

        @Override
        protected Void doInBackground(Void[] params) {

            /* Check minimum API level TODO not yet available from JSON. */

            /* Check version code. */
            MobileCenterLog.debug(LOG_TAG, "Check version code.");
            boolean isMoreRecent = false;
            PackageManager packageManager = mContext.getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
                if (mReleaseDetails.getVersion() > packageInfo.versionCode) {
                    MobileCenterLog.debug(LOG_TAG, "Latest release version code is higher.");
                    isMoreRecent = true;
                } else if (mReleaseDetails.getVersion() == packageInfo.versionCode) {

                    /* Check hash code to see if it's a different build. TODO */
                    MobileCenterLog.debug(LOG_TAG, "Same version code, need to check hash.");
                    isMoreRecent = false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                MobileCenterLog.error(LOG_TAG, "Could not compare versions.", e);
                return null;
            }

            /* Start download if build compatible with device and more recent. */
            if (isMoreRecent) {

                /* Download file. */
                Uri downloadUrl = mReleaseDetails.getDownloadUrl();
                MobileCenterLog.debug(LOG_TAG, "Start downloading new release, url=" + downloadUrl);
                DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(downloadUrl);
                long downloadRequestId = downloadManager.enqueue(request);
                storeDownloadRequestId(downloadManager, this, downloadRequestId);
            }
            return null;
        }
    }

    /**
     * Inspect a completed download, this uses APIs that would trigger strict mode violation if used in U.I. thread.
     */
    private class ProcessDownloadCompletion extends AsyncTask<Void, Void, Notification> {

        /**
         * Context.
         */
        private final Context mContext;

        /**
         * Download identifier to inspect.
         */
        private final long mDownloadId;

        /**
         * Init.
         *
         * @param context    context.
         * @param downloadId download identifier.
         */
        ProcessDownloadCompletion(Context context, long downloadId) {
            mContext = context;
            mDownloadId = downloadId;
        }

        @Override
        protected Notification doInBackground(Void... params) {

            /* Completion might be triggered before MobileCenter.start. */
            MobileCenterLog.debug(LOG_TAG, "Process download completion id=" + mDownloadId);
            if (Updates.this.mContext == null) {
                MobileCenterLog.debug(LOG_TAG, "Called before onStart, init storage");
                StorageHelper.initialize(mContext);
            }

            /* Check intent data is what we expected. */
            long expectedDownloadId = StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, -1);
            if (expectedDownloadId != mDownloadId) {
                MobileCenterLog.warn(LOG_TAG, "Ignoring completion for a download we didn't expect, id=" + mDownloadId);
                return null;
            }

            /* Check if download successful. */
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
            Uri uriForDownloadedFile = downloadManager.getUriForDownloadedFile(mDownloadId);
            if (uriForDownloadedFile != null) {

                /* Build install intent. */
                MobileCenterLog.debug(LOG_TAG, "Download was successful for id=" + mDownloadId + " uri=" + uriForDownloadedFile);
                Intent intent = getInstallIntent(uriForDownloadedFile);

                /* Exit check point. */
                Activity activity;
                try {
                    activity = checkStateIsValidFor(this);
                } catch (IllegalStateException e) {

                    /* If we were canceled, exit now. */
                    return null;
                }

                /* If foreground, execute now, otherwise post notification. */
                if (activity != null) {

                    /* This start call triggers strict mode violation in U.I. thread so it needs to be done here, and we can't synchronize anymore... */
                    MobileCenterLog.debug(LOG_TAG, "Application is in foreground, launch install UI now.");
                    activity.startActivity(intent);
                } else {

                    /* Remember we have a download ready. */
                    MobileCenterLog.debug(LOG_TAG, "Application is in background, post a notification.");
                    StorageHelper.PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, uriForDownloadedFile.toString());

                    /* And notify. */
                    int icon;
                    try {
                        ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), 0);
                        icon = applicationInfo.icon;
                    } catch (PackageManager.NameNotFoundException e) {
                        MobileCenterLog.error(LOG_TAG, "Could not get application icon", e);
                        return null;
                    }
                    Notification.Builder builder = new Notification.Builder(mContext)
                            .setTicker(mContext.getString(R.string.mobile_center_updates_download_successful_notification_title))
                            .setContentTitle(mContext.getString(R.string.mobile_center_updates_download_successful_notification_title))
                            .setContentText(mContext.getString(R.string.mobile_center_updates_download_successful_notification_message))
                            .setSmallIcon(icon)
                            .setContentIntent(PendingIntent.getActivities(mContext, 0, new Intent[]{intent}, 0));
                    Notification notification;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        notification = builder.build();
                    } else {
                        //noinspection deprecation
                        notification = builder.getNotification();
                    }
                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                    notifyDownload(mContext, this, notification);
                }
            } else {
                MobileCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId);
            }
            return null;
        }
    }
}
