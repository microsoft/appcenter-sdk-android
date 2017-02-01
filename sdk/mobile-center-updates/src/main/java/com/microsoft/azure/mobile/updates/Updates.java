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

    private static final String PREFERENCE_KEY_DOWNLOAD_URI = PREFERENCE_PREFIX + "download_uri";

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

    private boolean mBrowserOpened;

    private HttpClient mHttpClient;

    private AsyncTask<Void, Void, Void> mCheckReleaseTask;

    private ServiceCall mCheckReleaseApiCall;

    private Object mCheckReleaseCallId;

    private String mUpdateToken;

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

    @NonNull
    private static Intent getInstallIntent(Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private static int getNotificationId(long downloadId) {
        return (Updates.class.getName() + downloadId).hashCode();
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
        checkWhatToDoNext();
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mForegroundActivity = activity;
        checkWhatToDoNext();
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        mForegroundActivity = null;
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        if (enabled) {
            checkWhatToDoNext();
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
            mBrowserOpened = false;
            long downloadId = StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID);
            if (downloadId > 0) {
                MobileCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
                DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
                downloadManager.remove(downloadId);
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(getNotificationId(downloadId));
            }
            StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
            StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
            StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        }
    }

    private synchronized void checkWhatToDoNext() {
        if (mForegroundActivity != null) {

            /* If we received the update token before Mobile Center was started/enabled, process it now. */
            if (mUpdateToken != null) {
                MobileCenterLog.debug(LOG_TAG, "Processing update token we kept in memory before onStarted");
                storeUpdateToken(mUpdateToken);
                mUpdateToken = null;
                return;
            }

            /* If we have a download ready but we were in background, pop install UI now. */
            try {
                Uri apkUri = Uri.parse(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_DOWNLOAD_URI));
                StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
                MobileCenterLog.debug(LOG_TAG, "Now in foreground, remove notification and start install for APK uri=" + apkUri);
                long downloadId = StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID);
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(getNotificationId(downloadId));
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
                checkUpdate(updateToken);
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
            mUpdateToken = updateToken;
        } else if (isInstanceEnabled()) {
            StorageHelper.PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, updateToken);
            MobileCenterLog.debug(LOG_TAG, "Stored update token.");
            if (mCheckReleaseCallId == null) {
                checkUpdate(updateToken);
            }
            // TODO else cancel request retry?
        }
    }

    private synchronized void checkUpdate(@NonNull String updateToken) {
        MobileCenterLog.debug(LOG_TAG, "Check latest release...");
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
            MobileCenterLog.debug(LOG_TAG, "Schedule background version check...");
            mCheckReleaseTask = new CheckReleaseDetails(releaseDetails).execute();
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
        if (mCheckReleaseTask == task && isInstanceEnabled()) {

            /* Delete previous download. */
            long previousDownloadId = StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID);
            if (previousDownloadId > 0) {
                MobileCenterLog.debug(LOG_TAG, "Delete previous download an notification id=" + previousDownloadId);
                downloadManager.remove(previousDownloadId);
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(getNotificationId(previousDownloadId));
            }

            /* Store new download identifier. */
            StorageHelper.PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadRequestId);
        } else {

            /* State changed quickly, cancel download. */
            MobileCenterLog.debug(LOG_TAG, "State changed while downloading, cancel id=" + downloadRequestId);
            downloadManager.remove(downloadRequestId);
        }
    }

    synchronized void resumeApp(Context context) {
        if (mForegroundActivity == null) {
            PackageManager packageManager = context.getPackageManager();
            Intent resumeIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            if (resumeIntent != null) {
                resumeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(resumeIntent);
            }
        }
    }

    synchronized void processCompletedDownload(Context context, long downloadId) {

        /* Completion might be triggered before MobileCenter.start. */
        MobileCenterLog.debug(LOG_TAG, "Process download completion id=" + downloadId);
        if (mContext == null) {
            MobileCenterLog.debug(LOG_TAG, "Called before onStart, init storage");
            StorageHelper.initialize(context);
        }

        /* Check intent data is what we expected. */
        long expectedDownloadId = StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, -1);
        if (expectedDownloadId != downloadId) {
            MobileCenterLog.warn(LOG_TAG, "Ignoring completion for a download we didn't expect, id=" + downloadId);
            return;
        }

        /* Check if download successful. */
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        Uri uriForDownloadedFile = downloadManager.getUriForDownloadedFile(downloadId);
        if (uriForDownloadedFile != null) {

            /* Build install intent. */
            MobileCenterLog.debug(LOG_TAG, "Download was successful for id=" + downloadId + " uri=" + uriForDownloadedFile);
            Intent intent = getInstallIntent(uriForDownloadedFile);

            /* If foreground, execute now, otherwise post notification. */
            if (mForegroundActivity != null) {
                MobileCenterLog.debug(LOG_TAG, "We are in foreground, launch install UI now.");
                mForegroundActivity.startActivity(intent);
            } else {

                /* Remember we have a download ready. */
                MobileCenterLog.debug(LOG_TAG, "We are in background, post a notification.");
                StorageHelper.PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, uriForDownloadedFile.toString());

                /* And notify. */
                int icon;
                try {
                    ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
                    icon = applicationInfo.icon;
                } catch (PackageManager.NameNotFoundException e) {
                    MobileCenterLog.error(LOG_TAG, "Could not get application icon", e);
                    return;
                }
                Notification.Builder builder = new Notification.Builder(context)
                        .setContentTitle(context.getString(R.string.mobile_center_updates_download_successful_notification_title))
                        .setContentText(context.getString(R.string.mobile_center_updates_download_successful_notification_message))
                        .setSmallIcon(icon)
                        .setWhen(System.currentTimeMillis())
                        .setContentIntent(PendingIntent.getActivities(context, 0, new Intent[]{intent}, 0));
                Notification notification;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    notification = builder.build();
                } else {
                    //noinspection deprecation
                    notification = builder.getNotification();
                }
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                int notificationId = getNotificationId(downloadId);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, notification);
            }
        } else {
            MobileCenterLog.error(LOG_TAG, "Failed to download update id=" + downloadId);
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
            MobileCenterLog.debug(LOG_TAG, "Check version code.");
            boolean isMoreRecent = false;
            PackageManager packageManager = mContext.getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
                if (mReleaseDetails.getVersion() > packageInfo.versionCode) {
                    MobileCenterLog.debug(LOG_TAG, "Latest release version code is higher.");
                    isMoreRecent = true;
                } else if (mReleaseDetails.getVersion() == packageInfo.versionCode) {
                    // FIXME check hash when version code is same
                    MobileCenterLog.debug(LOG_TAG, "Same version code, need to check hash TODO, for now we assume more recent.");
                    isMoreRecent = true;
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
}
