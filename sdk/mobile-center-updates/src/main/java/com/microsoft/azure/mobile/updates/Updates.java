package com.microsoft.azure.mobile.updates;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.DefaultHttpClient;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.HttpClientRetryer;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.AsyncTaskUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;
import com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.azure.mobile.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.azure.mobile.updates.UpdateConstants.DEFAULT_API_URL;
import static com.microsoft.azure.mobile.updates.UpdateConstants.DEFAULT_INSTALL_URL;
import static com.microsoft.azure.mobile.updates.UpdateConstants.GET_LATEST_RELEASE_PATH_FORMAT;
import static com.microsoft.azure.mobile.updates.UpdateConstants.HEADER_API_TOKEN;
import static com.microsoft.azure.mobile.updates.UpdateConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.azure.mobile.updates.UpdateConstants.LOG_TAG;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_PLATFORM;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_PLATFORM_VALUE;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_REDIRECT_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_RELEASE_HASH;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PARAMETER_REQUEST_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_URI;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_IGNORED_RELEASE_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.azure.mobile.updates.UpdateConstants.SERVICE_NAME;
import static com.microsoft.azure.mobile.updates.UpdateConstants.UPDATE_SETUP_PATH_FORMAT;

/**
 * Updates service.
 */
public class Updates extends AbstractMobileCenterService {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Updates sInstance = null;

    /**
     * Current install base URL.
     */
    private String mInstallUrl = DEFAULT_INSTALL_URL;

    /**
     * Current API base URL.
     */
    private String mApiUrl = DEFAULT_API_URL;

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
     * Remember if we already tried to open the browser to update setup.
     */
    private boolean mBrowserOpenedOrAborted;

    /**
     * In memory token if we receive deep link intent before onStart.
     */
    private String mBeforeStartUpdateToken;

    /**
     * In memory request identifier if we receive deep link intent before onStart.
     */
    private String mBeforeStartRequestId;

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
     * Latest release details waiting to be shown to user.
     */
    private ReleaseDetails mReleaseDetails;

    /**
     * Last update dialog that was shown.
     */
    private AlertDialog mUpdateDialog;

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private DownloadTask mDownloadTask;

    /**
     * Current task to process download completion.
     */
    private ProcessDownloadCompletionTask mProcessDownloadCompletionTask;

    /**
     * True when update workflow reached final state.
     * This can be reset to check update again when app restarts.
     */
    private boolean mWorkflowCompleted;

    /**
     * Cache launch intent not to resolve it every time from package manager in every onCreate call.
     */
    private String mLauncherActivityClassName;

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
    @SuppressWarnings("WeakerAccess")
    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable Updates service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    @SuppressWarnings("WeakerAccess")
    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Change the base URL opened in the browser to get update token from user login information.
     *
     * @param installUrl install base URL.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public static void setInstallUrl(String installUrl) {
        getInstance().setInstanceInstallUrl(installUrl);
    }

    /**
     * Change the base URL used to make API calls.
     *
     * @param apiUrl API base URL.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public static void setApiUrl(String apiUrl) {
        getInstance().setInstanceApiUrl(apiUrl);
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
    @VisibleForTesting
    static int getNotificationId() {
        return Updates.class.getName().hashCode();
    }

    @SuppressWarnings("deprecation")
    private static Uri getFileUriOnOldDevices(Cursor cursor) {
        return Uri.parse("file://" + cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)));
    }

    @SuppressWarnings("deprecation")
    private static Notification buildNotification(Notification.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return builder.build();
        } else {
            return builder.getNotification();
        }
    }

    /**
     * Get download identifier from storage.
     *
     * @return download identifier or negative value if not found.
     */
    private static long getStoredDownloadId() {
        return StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
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
    public synchronized void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        /* Resolve launcher class name only once, use empty string to cache a failed resolution. */
        if (mLauncherActivityClassName == null) {
            mLauncherActivityClassName = "";
            PackageManager packageManager = activity.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(activity.getPackageName());
            if (intent != null) {
                mLauncherActivityClassName = intent.resolveActivity(packageManager).getClassName();
            }
        }

        /* Clear workflow finished state if launch recreated, to achieve check on "startup". */
        if (activity.getClass().getName().equals(mLauncherActivityClassName)) {
            MobileCenterLog.info(LOG_TAG, "Launcher activity restarted.");
            if (PreferencesStorage.getString(PREFERENCE_KEY_DOWNLOAD_URI) == null) {
                mWorkflowCompleted = false;
                mBrowserOpenedOrAborted = false;
            }
        }
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
            mBrowserOpenedOrAborted = false;
            mWorkflowCompleted = false;
            cancelPreviousTasks();
            PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
            PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
            PreferencesStorage.remove(PREFERENCE_KEY_IGNORED_RELEASE_ID);
        }
    }

    /**
     * Implements {@link #setInstallUrl(String)}.
     */
    private synchronized void setInstanceInstallUrl(String installUrl) {
        mInstallUrl = installUrl;
    }

    /**
     * Implements {@link #setApiUrl(String)}}.
     */
    private synchronized void setInstanceApiUrl(String apiUrl) {
        mApiUrl = apiUrl;
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
        mUpdateDialog = null;
        mReleaseDetails = null;
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        if (mProcessDownloadCompletionTask != null) {
            mProcessDownloadCompletionTask.cancel(true);
            mProcessDownloadCompletionTask = null;
        }
        long downloadId = getStoredDownloadId();
        if (downloadId >= 0) {
            MobileCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
            removeDownload(downloadId);
        }
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    /**
     * Method that triggers the update workflow or proceed to the next step.
     */
    private synchronized void resumeUpdateWorkflow() {
        if (mForegroundActivity != null && !mWorkflowCompleted && isInstanceEnabled()) {

            /* If we received the update token before Mobile Center was started/enabled, process it now. */
            if (mBeforeStartUpdateToken != null) {
                MobileCenterLog.debug(LOG_TAG, "Processing update token we kept in memory before onStarted");
                storeUpdateToken(mBeforeStartUpdateToken, mBeforeStartRequestId);
                mBeforeStartUpdateToken = null;
                mBeforeStartRequestId = null;
                return;
            }

            /* If we have a download ready but we were in background, pop install UI now. */
            String downloadUri = PreferencesStorage.getString(PREFERENCE_KEY_DOWNLOAD_URI);
            if ("".equals(downloadUri)) {

                /* TODO double check that with download manager. */
                MobileCenterLog.verbose(LOG_TAG, "Download is still in progress...");
                return;
            } else if (downloadUri != null) {
                try {

                    /* FIXME this can cause strict mode violation. */
                    Uri apkUri = Uri.parse(downloadUri);
                    MobileCenterLog.debug(LOG_TAG, "Now in foreground, remove notification and start install for APK uri=" + apkUri);
                    mForegroundActivity.startActivity(getInstallIntent(apkUri));
                    NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(getNotificationId());
                    completeWorkflow();
                    return;
                } catch (ActivityNotFoundException e) {

                    /* Cleanup on exception and resume update workflow. */
                    MobileCenterLog.warn(LOG_TAG, "Download uri was invalid", e);
                    cancelPreviousTasks();
                }
            }

            /* If we were waiting after API call to resume app to show the dialog do it now. */
            if (mReleaseDetails != null) {
                showUpdateDialog();
                return;
            }

            /* Nothing more to do for now if we are already calling API to check release. */
            if (mCheckReleaseCallId != null) {
                MobileCenterLog.verbose(LOG_TAG, "Already checking or checked latest release.");
                return;
            }

            /* Check if we have previous stored the update token. */
            String updateToken = PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN);
            if (updateToken != null) {
                getLatestReleaseDetails(updateToken);
                return;
            }

             /* If not, open browser to update setup. */
            if (mBrowserOpenedOrAborted) {
                return;
            }

            /* Compute hash. */
            String releaseHash;
            try {
                releaseHash = computeHash(mContext);
            } catch (PackageManager.NameNotFoundException e) {
                MobileCenterLog.error(LOG_TAG, "Could not get package info", e);
                mBrowserOpenedOrAborted = true;
                return;
            }

            /* Generate request identifier. */
            String requestId = UUIDUtils.randomUUID().toString();

            /* Build URL. */
            String url = mInstallUrl;
            url += String.format(UPDATE_SETUP_PATH_FORMAT, mAppSecret);
            url += "?" + PARAMETER_RELEASE_HASH + "=" + releaseHash;
            url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
            url += "&" + PARAMETER_REQUEST_ID + "=" + requestId;
            url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
            MobileCenterLog.debug(LOG_TAG, "No token, need to open browser to url=" + url);

            /* Store request id. */
            PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId);

            /* Open browser, remember that whatever the outcome to avoid opening it twice. */
            BrowserUtils.openBrowser(url, mForegroundActivity);
            mBrowserOpenedOrAborted = true;
        }
    }

    @NonNull
    private String computeHash(@NonNull Context context) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return computeHash(context, packageInfo);
    }

    @NonNull
    private String computeHash(@NonNull Context context, @NonNull PackageInfo packageInfo) {
        // TODO switch to the following hash when backend supports it: HashUtils.sha256(context.getPackageName() + ":" + packageInfo.versionName + ":" + packageInfo.versionCode);
        return context.getString(packageInfo.applicationInfo.labelRes);
    }

    /**
     * Reset all variables that matter to restart checking a new release on launcher activity restart.
     *
     * @param releaseDetails to check if state changed and that the call should be ignored.
     */
    private synchronized void completeWorkflow(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            completeWorkflow();
        }
    }

    /**
     * Reset all variables that matter to restart checking a new release on launcher activity restart.
     *
     * @param task to check if state changed and that the call should be ignored.
     */
    private synchronized void completeWorkflow(ProcessDownloadCompletionTask task) {
        if (task == mProcessDownloadCompletionTask) {
            completeWorkflow();
        }
    }

    /**
     * Reset all variables that matter to restart checking a new release on launcher activity restart.
     */
    private synchronized void completeWorkflow() {
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        mCheckReleaseApiCall = null;
        mCheckReleaseCallId = null;
        mUpdateDialog = null;
        mReleaseDetails = null;
        mWorkflowCompleted = true;
    }

    /*
     * Store update token and possibly trigger application update check.
     * TODO encrypt token, but where to store encryption key? If it's retrieved from server,
     * how do we protect server call to get the key in the first place?
     * Even having the encryption key temporarily in memory is risky as that can be heap dumped.
     */
    synchronized void storeUpdateToken(@NonNull String updateToken, @NonNull String requestId) {

        /* Keep token for later if we are not started and enabled yet. */
        if (mContext == null) {
            MobileCenterLog.debug(LOG_TAG, "Update token received before onStart, keep it in memory.");
            mBeforeStartUpdateToken = updateToken;
            mBeforeStartRequestId = requestId;
        } else if (requestId.equals(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID))) {
            PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, updateToken);
            PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
            MobileCenterLog.debug(LOG_TAG, "Stored update token.");
            cancelPreviousTasks();
            getLatestReleaseDetails(updateToken);
        } else {
            MobileCenterLog.warn(LOG_TAG, "Ignoring update token as requestId is invalid.");
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
        String url = mApiUrl + String.format(GET_LATEST_RELEASE_PATH_FORMAT, mAppSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_API_TOKEN, updateToken);
        final Object releaseCallId = mCheckReleaseCallId = new Object();
        mCheckReleaseApiCall = httpClient.callAsync(url, METHOD_GET, headers, null, new ServiceCallback() {

            @Override
            public void onCallSucceeded(String payload) {
                try {
                    handleApiCallSuccess(releaseCallId, ReleaseDetails.parse(payload));
                } catch (JSONException e) {
                    onCallFailed(e);
                }
            }

            @Override
            public void onCallFailed(Exception e) {
                handleApiCallFailure(releaseCallId, e);
            }
        });
    }

    /**
     * Handle API call failure.
     */
    private synchronized void handleApiCallFailure(Object releaseCallId, Exception e) {

        /* Check if state did not change. */
        if (mCheckReleaseCallId == releaseCallId) {
            MobileCenterLog.error(LOG_TAG, "Failed to check latest release:", e);
            completeWorkflow();
        }
    }

    /**
     * Handle API call success.
     */
    private synchronized void handleApiCallSuccess(Object releaseCallId, ReleaseDetails releaseDetails) {

        /* Check if state did not change. */
        if (mCheckReleaseCallId == releaseCallId) {

            /* Check ignored. */
            String releaseId = releaseDetails.getId();
            if (releaseId.equals(PreferencesStorage.getString(PREFERENCE_KEY_IGNORED_RELEASE_ID))) {
                MobileCenterLog.debug(LOG_TAG, "This release is ignored id=" + releaseId);
            } else {

                /* Check version code is equals or higher and hash is different. */
                MobileCenterLog.debug(LOG_TAG, "Check version code.");
                PackageManager packageManager = mContext.getPackageManager();
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
                    if (isMoreRecent(packageInfo, releaseDetails)) {

                        /* Show update dialog. */
                        mReleaseDetails = releaseDetails;
                        if (mForegroundActivity != null) {
                            showUpdateDialog();
                        }
                        return;
                    } else {
                        MobileCenterLog.debug(LOG_TAG, "Latest server version is not more recent.");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    MobileCenterLog.error(LOG_TAG, "Could not compare versions.", e);
                }
            }

            /* If update dialog was not shown or scheduled, complete workflow. */
            completeWorkflow();
        }
    }

    /**
     * Check if the fetched release information should be installed.
     *
     * @param packageInfo    current app version.
     * @param releaseDetails latest release on server.
     * @return true if latest release on server should be used.
     */
    private boolean isMoreRecent(PackageInfo packageInfo, ReleaseDetails releaseDetails) {
//        TODO when releaseHash is exposed in JSON.
//        if (releaseDetails.getVersion() == packageInfo.versionCode) {
//            return !releaseDetails.getReleaseHash().equals(computeHash(mContext, packageInfo));
//        }
        return releaseDetails.getVersion() > packageInfo.versionCode;
    }

    /**
     * Show update dialog. This can be called multiple times if clicking on HOME and app resumed
     * (it could be resumed in another activity covering the previous one).
     */
    private synchronized void showUpdateDialog() {

        /* We could be in another activity now, refresh dialog. */
        MobileCenterLog.debug(LOG_TAG, "Show update dialog.");
        if (mUpdateDialog != null) {
            mUpdateDialog.hide();
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
        dialogBuilder.setTitle(R.string.mobile_center_updates_update_dialog_title);
        final ReleaseDetails releaseDetails = mReleaseDetails;
        String releaseNotes = releaseDetails.getReleaseNotes();
        if (TextUtils.isEmpty(releaseNotes))
            dialogBuilder.setMessage(R.string.mobile_center_updates_update_dialog_message);
        else
            dialogBuilder.setMessage(releaseNotes);
        dialogBuilder.setPositiveButton(R.string.mobile_center_updates_update_dialog_download, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                scheduleDownload(releaseDetails);
            }
        });
        dialogBuilder.setNegativeButton(R.string.mobile_center_updates_update_dialog_ignore, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ignoreRelease(releaseDetails);
            }
        });
        dialogBuilder.setNeutralButton(R.string.mobile_center_updates_update_dialog_postpone, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                completeWorkflow(releaseDetails);
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                completeWorkflow(releaseDetails);
            }
        });
        mUpdateDialog = dialogBuilder.create();
        mUpdateDialog.show();
    }

    /**
     * Ignore the specified release. It won't be prompted anymore until another release is available.
     *
     * @param releaseDetails release details.
     */
    private synchronized void ignoreRelease(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            String id = releaseDetails.getId();
            MobileCenterLog.debug(LOG_TAG, "Ignore release id=" + id);
            PreferencesStorage.putString(PREFERENCE_KEY_IGNORED_RELEASE_ID, id);
            completeWorkflow();
        }
    }

    /**
     * Check state did not change and schedule download of the release.
     *
     * @param releaseDetails release details.
     */
    private synchronized void scheduleDownload(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            MobileCenterLog.debug(LOG_TAG, "Schedule download...");
            mDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadTask(releaseDetails));
        }
    }

    /**
     * Persist download state.
     *
     * @param downloadManager   download manager.
     * @param task              current task to check race conditions.
     * @param downloadRequestId download identifier.
     */
    @WorkerThread
    private synchronized void storeDownloadRequestId(DownloadManager downloadManager, DownloadTask task, long downloadRequestId) {

        /* Check for if state changed and task not canceled in time. */
        if (mDownloadTask == task) {

            /* Delete previous download. */
            long previousDownloadId = getStoredDownloadId();
            if (previousDownloadId >= 0) {
                MobileCenterLog.debug(LOG_TAG, "Delete previous download id=" + previousDownloadId);
                downloadManager.remove(previousDownloadId);
            }

            /* Store new download identifier. */
            PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadRequestId);
            PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, "");
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
        mProcessDownloadCompletionTask = AsyncTaskUtils.execute(LOG_TAG, new ProcessDownloadCompletionTask(context, downloadId));
    }

    /**
     * Used by task processing the download completion in background prior to showing install U.I to check if request was canceled.
     *
     * @param task task to check state for.
     * @return foreground activity if any, if state is valid.
     * @throws IllegalStateException if state changed.
     */
    private synchronized Activity getForegroundActivityWithStateCheck(ProcessDownloadCompletionTask task) throws IllegalStateException {
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
     * @param uri          uri to persist to remember download completed state.
     */
    private synchronized void notifyDownload(Context context, ProcessDownloadCompletionTask task, Notification notification, String uri) {
        if (task == mProcessDownloadCompletionTask) {
            PreferencesStorage.putString(PREFERENCE_KEY_DOWNLOAD_URI, uri);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(getNotificationId(), notification);
        }
    }

    /**
     * Remove a previously downloaded file and any notification.
     */
    private void removeDownload(long downloadId) {
        MobileCenterLog.debug(LOG_TAG, "Delete previous notification downloadId=" + downloadId);
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(getNotificationId());
        AsyncTaskUtils.execute(LOG_TAG, new RemoveDownloadTask(), downloadId);
    }

    /**
     * Removing a download violates strict mode in U.I. thread.
     */
    @VisibleForTesting
    class RemoveDownloadTask extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(Long... params) {

            /* This special cleanup task does not require any cancellation on state change as a previous download will never be reused. */
            Long downloadId = params[0];
            MobileCenterLog.debug(LOG_TAG, "Delete previous download downloadId=" + downloadId);
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.remove(downloadId);
            return null;
        }
    }

    /**
     * The download manager API violates strict mode in U.I. thread.
     */
    @VisibleForTesting
    class DownloadTask extends AsyncTask<Void, Void, Void> {

        /**
         * Release details to check.
         */
        private final ReleaseDetails mReleaseDetails;

        /**
         * Init.
         *
         * @param releaseDetails release details associated to this check.
         */
        DownloadTask(ReleaseDetails releaseDetails) {
            mReleaseDetails = releaseDetails;
        }

        @Override
        protected Void doInBackground(Void[] params) {

            /* Download file. */
            Uri downloadUrl = mReleaseDetails.getDownloadUrl();
            MobileCenterLog.debug(LOG_TAG, "Start downloading new release, url=" + downloadUrl);
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(downloadUrl);
            long downloadRequestId = downloadManager.enqueue(request);
            storeDownloadRequestId(downloadManager, this, downloadRequestId);
            return null;
        }
    }

    /**
     * Inspect a completed download, this uses APIs that would trigger strict mode violation if used in U.I. thread.
     */
    @VisibleForTesting
    class ProcessDownloadCompletionTask extends AsyncTask<Void, Void, Void> {

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
        ProcessDownloadCompletionTask(Context context, long downloadId) {
            mContext = context;
            mDownloadId = downloadId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            /*
             * Completion might be triggered in background before MobileCenter.start
             * if application was killed after starting download.
             *
             * We still want to generate the notification: if we can find the data in preferences
             * that means they were not deleted, and thus that the sdk was not disabled.
             */
            MobileCenterLog.debug(LOG_TAG, "Process download completion id=" + mDownloadId);
            if (Updates.this.mContext == null) {
                MobileCenterLog.debug(LOG_TAG, "Called before onStart, init storage");
                StorageHelper.initialize(mContext);
            }

            /* Check intent data is what we expected. */
            long expectedDownloadId = getStoredDownloadId();
            if (expectedDownloadId >= 0 && expectedDownloadId != mDownloadId) {
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
                boolean installerFound = false;
                if (intent.resolveActivity(mContext.getPackageManager()) == null) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
                        if (cursor != null) {
                            if (cursor.moveToNext()) {
                                uriForDownloadedFile = getFileUriOnOldDevices(cursor);
                                intent = getInstallIntent(uriForDownloadedFile);
                                installerFound = intent.resolveActivity(mContext.getPackageManager()) != null;
                            }
                            cursor.close();
                        }
                    }
                } else {
                    installerFound = true;
                }
                if (!installerFound) {
                    MobileCenterLog.error(LOG_TAG, "Installer not found");
                    completeWorkflow(this);
                    return null;
                }

                /* Exit check point. */
                Activity activity;
                try {
                    activity = getForegroundActivityWithStateCheck(this);
                } catch (IllegalStateException e) {

                    /* If we were canceled, exit now. */
                    return null;
                }

                /* If foreground, execute now, otherwise post notification. */
                String uri = uriForDownloadedFile.toString();
                if (activity != null) {

                    /* This start call triggers strict mode violation in U.I. thread so it needs to be done here, and we can't synchronize anymore... */
                    MobileCenterLog.debug(LOG_TAG, "Application is in foreground, launch install UI now.");
                    activity.startActivity(intent);
                    completeWorkflow(this);
                } else {

                    /* Remember we have a download ready. */
                    MobileCenterLog.debug(LOG_TAG, "Application is in background, post a notification.");

                    /* And notify. */
                    int icon;
                    try {
                        ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), 0);
                        icon = applicationInfo.icon;
                    } catch (PackageManager.NameNotFoundException e) {
                        MobileCenterLog.error(LOG_TAG, "Could not get application icon", e);
                        completeWorkflow(this);
                        return null;
                    }
                    Notification.Builder builder = new Notification.Builder(mContext)
                            .setTicker(mContext.getString(R.string.mobile_center_updates_download_successful_notification_title))
                            .setContentTitle(mContext.getString(R.string.mobile_center_updates_download_successful_notification_title))
                            .setContentText(mContext.getString(R.string.mobile_center_updates_download_successful_notification_message))
                            .setSmallIcon(icon)
                            .setContentIntent(PendingIntent.getActivities(mContext, 0, new Intent[]{intent}, 0));
                    Notification notification;
                    notification = buildNotification(builder);
                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                    notifyDownload(mContext, this, notification, uri);
                }
            } else {
                MobileCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId);
                completeWorkflow(this);
            }
            return null;
        }
    }
}
