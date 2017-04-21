package com.microsoft.azure.mobile.distribute;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.widget.Toast;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.DefaultHttpClient;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.HttpClientRetryer;
import com.microsoft.azure.mobile.http.HttpException;
import com.microsoft.azure.mobile.http.HttpUtils;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.AsyncTaskUtils;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;
import com.microsoft.azure.mobile.utils.crypto.CryptoUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;
import com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import static android.util.Log.VERBOSE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.DEFAULT_API_URL;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.DEFAULT_INSTALL_URL;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.DOWNLOAD_STATE_AVAILABLE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.DOWNLOAD_STATE_INSTALLING;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.DOWNLOAD_STATE_NOTIFIED;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.GET_LATEST_RELEASE_PATH_FORMAT;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.HANDLER_TOKEN_CHECK_PROGRESS;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.HEADER_API_TOKEN;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.MEBIBYTE_IN_BYTES;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.POSTPONE_TIME_THRESHOLD;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_POSTPONE_TIME;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.SERVICE_NAME;
import static com.microsoft.azure.mobile.distribute.DistributeUtils.computeReleaseHash;
import static com.microsoft.azure.mobile.distribute.DistributeUtils.getStoredDownloadState;
import static com.microsoft.azure.mobile.http.DefaultHttpClient.METHOD_GET;


/**
 * Distribute service.
 */
public class Distribute extends AbstractMobileCenterService {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Distribute sInstance;

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
     * Package info.
     */
    private PackageInfo mPackageInfo;

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
    private Dialog mUpdateDialog;

    /**
     * Last unknown sources dialog that was shown.
     */
    private Dialog mUnknownSourcesDialog;

    /**
     * Last download progress dialog that was shown.
     */
    private ProgressDialog mProgressDialog;

    /**
     * Mandatory download completed in app notification.
     */
    private Dialog mCompletedDownloadDialog;

    /**
     * Last activity that did show a dialog.
     * Used to avoid replacing a dialog in same screen as it causes flickering.
     */
    private WeakReference<Activity> mLastActivityWithDialog = new WeakReference<>(null);

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private DownloadTask mDownloadTask;

    /**
     * Current task to check download state and act on it.
     */
    private CheckDownloadTask mCheckDownloadTask;

    /**
     * Remember if we checked download since our own process restarted.
     */
    private boolean mCheckedDownload;

    /**
     * True when distribute workflow reached final state.
     * This can be reset to check update again when app restarts.
     */
    private boolean mWorkflowCompleted;

    /**
     * Cache launch intent not to resolve it every time from package manager in every onCreate call.
     */
    private String mLauncherActivityClassName;

    /**
     * Custom listener if any.
     */
    private DistributeListener mListener;

    /**
     * Flag to remember whether update dialog was customized or not.
     * Value is null when the current state is not {@link DistributeConstants#DOWNLOAD_STATE_AVAILABLE}
     * or was never in foreground after new release detected.
     */
    private Boolean mUsingDefaultUpdateDialog;

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Distribute getInstance() {
        if (sInstance == null) {
            sInstance = new Distribute();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check whether Distribute service is enabled or not.
     *
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable Distribute service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Change the base URL opened in the browser to get update token from user login information.
     *
     * @param installUrl install base URL.
     */
    public static void setInstallUrl(String installUrl) {
        getInstance().setInstanceInstallUrl(installUrl);
    }

    /**
     * Change the base URL used to make API calls.
     *
     * @param apiUrl API base URL.
     */
    public static void setApiUrl(String apiUrl) {
        getInstance().setInstanceApiUrl(apiUrl);
    }

    /**
     * Sets a distribute listener.
     *
     * @param listener The custom distribute listener.
     */
    public static void setListener(DistributeListener listener) {
        getInstance().setInstanceListener(listener);
    }

    /**
     * If update dialog is customized by returning <code>true</code> in  {@link DistributeListener#onReleaseAvailable(Activity, ReleaseDetails)},
     * You need to tell the distribute SDK using this function what is the user action.
     *
     * @param updateAction one of {@link UpdateAction} actions.
     *                     For mandatory updates, only {@link UpdateAction#UPDATE} is allowed.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static synchronized void notifyUpdateAction(@UpdateAction int updateAction) {
        getInstance().handleUpdateAction(updateAction);
    }

    @Override
    protected String getGroupName() {
        return null;
    }

    @Override
    public String getServiceName() {
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
        try {
            mPackageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            MobileCenterLog.error(LOG_TAG, "Could not get self package info.", e);
        }
        resumeDistributeWorkflow();
    }

    /**
     * Set context, used when need to manipulate context before onStarted.
     * For example when download completes after application process exited.
     */
    synchronized ReleaseDetails startFromBackground(Context context) {
        if (mAppSecret == null) {
            MobileCenterLog.debug(LOG_TAG, "Called before onStart, init storage");
            mContext = context;
            StorageHelper.initialize(mContext);
            mReleaseDetails = DistributeUtils.loadCachedReleaseDetails();
        }
        return mReleaseDetails;
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
            if (getStoredDownloadState() == DOWNLOAD_STATE_COMPLETED) {
                mWorkflowCompleted = false;
                mBrowserOpenedOrAborted = false;
            }
        }
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mForegroundActivity = activity;
        resumeDistributeWorkflow();
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        mForegroundActivity = null;
        hideProgressDialog();
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        if (enabled) {
            resumeDistributeWorkflow();
        } else {

            /* Clean all state on disabling, cancel everything. Keep token though. */
            mBrowserOpenedOrAborted = false;
            mWorkflowCompleted = false;
            cancelPreviousTasks();
            PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
            PreferencesStorage.remove(PREFERENCE_KEY_POSTPONE_TIME);
        }
    }

    /**
     * Implements {@link #notifyUpdateAction(int)}.
     */
    @VisibleForTesting
    synchronized void handleUpdateAction(int updateAction) {
        if (!isEnabled()) {
            MobileCenterLog.error(LOG_TAG, "Distribute is disabled");
            return;
        }
        if (getStoredDownloadState() != DOWNLOAD_STATE_AVAILABLE) {
            MobileCenterLog.error(LOG_TAG, "Cannot handler user update action at this time.");
            return;
        }
        if (mUsingDefaultUpdateDialog) {
            MobileCenterLog.error(LOG_TAG, "Cannot handler user update action when using default dialog.");
            return;
        }
        switch (updateAction) {

            case UpdateAction.UPDATE:
                enqueueDownloadOrShowUnknownSourcesDialog(mReleaseDetails);
                break;

            case UpdateAction.POSTPONE:
                if (mReleaseDetails.isMandatoryUpdate()) {
                    MobileCenterLog.error(LOG_TAG, "Cannot postpone a mandatory update.");
                    return;
                }
                postponeRelease(mReleaseDetails);
                break;

            default:
                MobileCenterLog.error(LOG_TAG, "Invalid update action: " + updateAction);
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
     * Implements {@link #setListener(DistributeListener)}.
     */
    private synchronized void setInstanceListener(DistributeListener listener) {
        mListener = listener;
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
        mUnknownSourcesDialog = null;
        mProgressDialog = null;
        mCompletedDownloadDialog = null;
        mLastActivityWithDialog.clear();
        mUsingDefaultUpdateDialog = null;
        mReleaseDetails = null;
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        if (mCheckDownloadTask != null) {
            mCheckDownloadTask.cancel(true);
            mCheckDownloadTask = null;
        }
        mCheckedDownload = false;
        long downloadId = DistributeUtils.getStoredDownloadId();
        if (downloadId >= 0) {
            MobileCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
            removeDownload(downloadId);
        }
        PreferencesStorage.remove(PREFERENCE_KEY_RELEASE_DETAILS);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_TIME);
    }

    /**
     * Method that triggers the distribute workflow or proceed to the next step.
     */
    private synchronized void resumeDistributeWorkflow() {
        if (mPackageInfo != null && mForegroundActivity != null && !mWorkflowCompleted && isInstanceEnabled()) {

            /* Don't go any further it this is a debug app. */
            if ((mContext.getApplicationInfo().flags & FLAG_DEBUGGABLE) == FLAG_DEBUGGABLE) {
                MobileCenterLog.info(LOG_TAG, "Not checking in app updates in debug.");
                mWorkflowCompleted = true;
                return;
            }

            /* Don't go any further if the app was installed from an app store. */
            if (InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext)) {
                MobileCenterLog.info(LOG_TAG, "Not checking in app updates as installed from a store.");
                mWorkflowCompleted = true;
                return;
            }

            /* If we received the update token before Mobile Center was started/enabled, process it now. */
            if (mBeforeStartUpdateToken != null) {
                MobileCenterLog.debug(LOG_TAG, "Processing update token we kept in memory before onStarted");
                storeUpdateToken(mBeforeStartUpdateToken, mBeforeStartRequestId);
                mBeforeStartUpdateToken = null;
                mBeforeStartRequestId = null;
                return;
            }

            /* Load cached release details if process restarted and we have such a cache. */
            int downloadState = getStoredDownloadState();
            if (mReleaseDetails == null && downloadState != DOWNLOAD_STATE_COMPLETED) {
                mReleaseDetails = DistributeUtils.loadCachedReleaseDetails();

                /* If cached release is optional and we have network, we should not reuse it. */
                if (mReleaseDetails != null && !mReleaseDetails.isMandatoryUpdate() &&
                        NetworkStateHelper.getSharedInstance(mContext).isNetworkConnected() &&
                        downloadState == DOWNLOAD_STATE_AVAILABLE) {
                    cancelPreviousTasks();
                }
            }

            /* If process restarted during workflow. */
            if (downloadState != DOWNLOAD_STATE_COMPLETED && downloadState != DOWNLOAD_STATE_AVAILABLE && !mCheckedDownload) {

                /* Discard release if application updated. Then immediately check release. */
                if (mPackageInfo.lastUpdateTime > StorageHelper.PreferencesStorage.getLong(PREFERENCE_KEY_DOWNLOAD_TIME)) {
                    MobileCenterLog.debug(LOG_TAG, "Discarding previous download as application updated.");
                    cancelPreviousTasks();
                }

                /* Otherwise check currently processed release. */
                else {

                    /* If app restarted, check if download completed to bring install U.I. */
                    mCheckedDownload = true;
                    checkDownload(mContext, DistributeUtils.getStoredDownloadId(), false);

                    /* If downloading mandatory update proceed to restore progress dialog in the meantime. */
                    if (mReleaseDetails == null || !mReleaseDetails.isMandatoryUpdate() || downloadState != DOWNLOAD_STATE_ENQUEUED) {
                        return;
                    }
                }
            }

            /*
             * If we got a release information but application backgrounded then resumed,
             * check what dialog to restore.
             */
            if (mReleaseDetails != null) {

                /* If we go back to application without installing the mandatory update. */
                if (downloadState == DOWNLOAD_STATE_INSTALLING) {

                    /* Show a new modal dialog with only install button. */
                    showMandatoryDownloadReadyDialog();
                }

                /* If we are still downloading. */
                else if (downloadState == DOWNLOAD_STATE_ENQUEUED) {

                    /* Refresh mandatory dialog progress or do nothing otherwise. */
                    if (mReleaseDetails.isMandatoryUpdate()) {
                        showDownloadProgress();
                        checkDownload(mContext, DistributeUtils.getStoredDownloadId(), true);
                    }
                }

                /* If we were showing unknown sources dialog, restore it. */
                else if (mUnknownSourcesDialog != null) {

                    /*
                     * Resume click download step if last time we were showing unknown source dialog.
                     * Note that we could be executed here after going to enable settings and being back in app.
                     * We can start download if the setting is now enabled,
                     * otherwise restore dialog if activity rotated or was covered.
                     */
                    enqueueDownloadOrShowUnknownSourcesDialog(mReleaseDetails);
                }

                /* Or restore update dialog if that's the last thing we did before being paused. */
                else {
                    showUpdateDialog();
                }

                /*
                 * Normally we would stop processing here after showing/restoring a dialog.
                 * But if we keep restoring a dialog for an update, we should still
                 * check in background if this release is replaced by a more recent one.
                 * Do that extra release check if app restarted AND we are
                 * displaying either an update/unknown sources dialog OR the install dialog.
                 * Basically if we are still downloading an update, we won't check a new one.
                 */
                if (downloadState != DOWNLOAD_STATE_AVAILABLE && downloadState != DOWNLOAD_STATE_INSTALLING) {
                    return;
                }
            }

            /* Nothing more to do for now if we are already calling API to check release. */
            if (mCheckReleaseCallId != null) {
                MobileCenterLog.verbose(LOG_TAG, "Already checking or checked latest release.");
                return;
            }

            /* Check if we have previous stored the update token. */
            String updateToken = PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN);
            if (updateToken != null) {

                /* Decrypt token. */
                CryptoUtils.DecryptedData decryptedData = CryptoUtils.getInstance(mContext).decrypt(updateToken);
                String newEncryptedData = decryptedData.getNewEncryptedData();

                /* Store new encrypted value if updated. */
                if (newEncryptedData != null) {
                    PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, newEncryptedData);
                }

                /* Check latest release. */
                getLatestReleaseDetails(decryptedData.getDecryptedData());
                return;
            }

             /* If not, open browser to update setup. */
            if (!mBrowserOpenedOrAborted) {
                DistributeUtils.updateSetupUsingBrowser(mForegroundActivity, mInstallUrl, mAppSecret, mPackageInfo);
                mBrowserOpenedOrAborted = true;
            }
        }
    }

    /**
     * Reset all variables that matter to restart checking a new release on launcher activity restart.
     *
     * @param releaseDetails to check if state changed and that the call should be ignored.
     */
    synchronized void completeWorkflow(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            completeWorkflow();
        }
    }

    /**
     * Cancel notification if needed.
     */
    private synchronized void cancelNotification() {
        if (getStoredDownloadState() == DOWNLOAD_STATE_NOTIFIED) {
            MobileCenterLog.debug(LOG_TAG, "Delete notification");
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(DistributeUtils.getNotificationId());
        }
    }

    /**
     * Reset all variables that matter to restart checking a new release on launcher activity restart.
     */
    synchronized void completeWorkflow() {
        cancelNotification();
        PreferencesStorage.remove(PREFERENCE_KEY_RELEASE_DETAILS);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        mCheckReleaseApiCall = null;
        mCheckReleaseCallId = null;
        mUpdateDialog = null;
        mUnknownSourcesDialog = null;
        hideProgressDialog();
        mLastActivityWithDialog.clear();
        mUsingDefaultUpdateDialog = null;
        mReleaseDetails = null;
        mWorkflowCompleted = true;
    }

    /*
     * Store update token and possibly trigger application update check.
     */
    synchronized void storeUpdateToken(@NonNull String updateToken, @NonNull String requestId) {

        /* Keep token for later if we are not started and enabled yet. */
        if (mContext == null) {
            MobileCenterLog.debug(LOG_TAG, "Update token received before onStart, keep it in memory.");
            mBeforeStartUpdateToken = updateToken;
            mBeforeStartRequestId = requestId;
        } else if (requestId.equals(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID))) {
            String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(updateToken);
            PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, encryptedToken);
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
    @VisibleForTesting
    synchronized void getLatestReleaseDetails(@NonNull String updateToken) {
        MobileCenterLog.debug(LOG_TAG, "Get latest release details...");
        HttpClientRetryer retryer = new HttpClientRetryer(new DefaultHttpClient());
        NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(mContext);
        HttpClient httpClient = new HttpClientNetworkStateHandler(retryer, networkStateHelper);
        String url = mApiUrl + String.format(GET_LATEST_RELEASE_PATH_FORMAT, mAppSecret, computeReleaseHash(mPackageInfo));
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_API_TOKEN, updateToken);
        final Object releaseCallId = mCheckReleaseCallId = new Object();
        mCheckReleaseApiCall = httpClient.callAsync(url, METHOD_GET, headers, new HttpClient.CallTemplate() {

            @Override
            public String buildRequestBody() throws JSONException {

                /* Only GET is used by Distribute service. This method is never getting called. */
                return null;
            }

            @Override
            public void onBeforeCalling(URL url, Map<String, String> headers) {
                if (MobileCenterLog.getLogLevel() <= VERBOSE) {

                    /* Log url. */
                    String urlString = url.toString().replaceAll(mAppSecret, HttpUtils.hideSecret(mAppSecret));
                    MobileCenterLog.verbose(LOG_TAG, "Calling " + urlString + "...");

                    /* Log headers. */
                    Map<String, String> logHeaders = new HashMap<>(headers);
                    String apiToken = logHeaders.get(HEADER_API_TOKEN);
                    if (apiToken != null) {
                        logHeaders.put(HEADER_API_TOKEN, HttpUtils.hideSecret(apiToken));
                    }
                    MobileCenterLog.verbose(LOG_TAG, "Headers: " + logHeaders);
                }
            }
        }, new ServiceCallback() {

            @Override
            public void onCallSucceeded(final String payload) {

                /* onPostExecute is not always called on UI thread due to an old Android bug. */
                HandlerUtils.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            handleApiCallSuccess(releaseCallId, payload, ReleaseDetails.parse(payload));
                        } catch (JSONException e) {
                            onCallFailed(e);
                        }
                    }
                });
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

            /* Complete workflow in error. */
            completeWorkflow();

            /* Delete token on unrecoverable error. */
            if (!HttpUtils.isRecoverableError(e)) {

                /*
                 * Unless its a special case: 404 with json code that no release is found.
                 * Could happen by cleaning releases with remove button.
                 */
                String code = null;
                if (e instanceof HttpException) {
                    HttpException httpException = (HttpException) e;
                    try {

                        /* We actually don't care of the http code if JSON code is specified. */
                        ErrorDetails errorDetails = ErrorDetails.parse(httpException.getPayload());
                        code = errorDetails.getCode();
                    } catch (JSONException je) {
                        MobileCenterLog.verbose(LOG_TAG, "Cannot read the error as JSON", je);
                    }
                }
                if (ErrorDetails.NO_RELEASES_FOR_USER_CODE.equals(code)) {
                    MobileCenterLog.info(LOG_TAG, "No release available to the current user.");
                } else {
                    MobileCenterLog.error(LOG_TAG, "Failed to check latest release:", e);
                    PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
                }
            }
        }
    }

    /**
     * Handle API call success.
     */
    private synchronized void handleApiCallSuccess(Object releaseCallId, String rawReleaseDetails, ReleaseDetails releaseDetails) {

        /* Check if state did not change. */
        if (mCheckReleaseCallId == releaseCallId) {

            /* Check minimum Android API level. */
            mCheckReleaseApiCall = null;
            if (Build.VERSION.SDK_INT >= releaseDetails.getMinApiLevel()) {

                /* Check version code is equals or higher and hash is different. */
                MobileCenterLog.debug(LOG_TAG, "Check if latest release is more recent.");
                if (isMoreRecent(releaseDetails) && canUpdateNow(releaseDetails)) {

                    /* Update cache. */
                    PreferencesStorage.putString(PREFERENCE_KEY_RELEASE_DETAILS, rawReleaseDetails);

                    /* If previous release is mandatory and still processing, don't do anything right now. */
                    if (mReleaseDetails != null && mReleaseDetails.isMandatoryUpdate()) {
                        if (mReleaseDetails.getId() != releaseDetails.getId()) {
                            MobileCenterLog.debug(LOG_TAG, "Latest release is more recent than the previous mandatory.");
                            PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_AVAILABLE);
                        } else {
                            MobileCenterLog.debug(LOG_TAG, "The latest release is mandatory and already being processed.");
                        }
                        return;
                    }

                    /* Show update dialog. */
                    mReleaseDetails = releaseDetails;
                    MobileCenterLog.debug(LOG_TAG, "Latest release is more recent.");
                    PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_AVAILABLE);
                    if (mForegroundActivity != null) {
                        showUpdateDialog();
                    }
                    return;
                }
            } else {
                MobileCenterLog.info(LOG_TAG, "This device is not compatible with the latest release.");
            }

            /* If update dialog was not shown or scheduled, complete workflow. */
            completeWorkflow();
        }
    }

    /**
     * Check if the fetched release information should be installed.
     *
     * @param releaseDetails latest release on server.
     * @return true if latest release on server should be used.
     */
    private boolean isMoreRecent(ReleaseDetails releaseDetails) {
        boolean moreRecent;
        if (releaseDetails.getVersion() == mPackageInfo.versionCode) {
            moreRecent = !releaseDetails.getReleaseHash().equals(DistributeUtils.computeReleaseHash(mPackageInfo));
        } else {
            moreRecent = releaseDetails.getVersion() > mPackageInfo.versionCode;
        }
        MobileCenterLog.debug(LOG_TAG, "Latest release more recent=" + moreRecent);
        return moreRecent;
    }

    /**
     * Check if release can be downloaded and installed now.
     *
     * @param releaseDetails release.
     * @return false if release optional AND user has clicked ask me in day since less than 24 hours ago, true otherwise.
     */
    private boolean canUpdateNow(ReleaseDetails releaseDetails) {
        if (releaseDetails.isMandatoryUpdate()) {
            MobileCenterLog.debug(LOG_TAG, "Release is mandatory, ignoring any postpone action.");
            return true;
        }
        long now = System.currentTimeMillis();
        long postponedTime = PreferencesStorage.getLong(PREFERENCE_KEY_POSTPONE_TIME, 0);
        if (now < postponedTime) {
            MobileCenterLog.debug(LOG_TAG, "User clock has been changed in past, cleaning postpone state and showing dialog");
            PreferencesStorage.remove(PREFERENCE_KEY_POSTPONE_TIME);
            return true;
        }
        long postponedUntil = postponedTime + POSTPONE_TIME_THRESHOLD;
        if (now < postponedUntil) {
            MobileCenterLog.debug(LOG_TAG, "Optional updates are postponed until " + new Date(postponedUntil));
            return false;
        }
        return true;
    }

    /**
     * Check if dialog should be restored in the new activity. Hiding previous dialog version if any.
     *
     * @param dialog existing dialog if any, always returning true when null.
     * @return true if a new dialog should be displayed, false otherwise.
     */
    private boolean shouldRefreshDialog(@Nullable Dialog dialog) {

        /* We could be in another activity now, refresh dialog. */
        if (dialog != null) {

            /* Nothing to if resuming same activity with dialog already displayed. */
            if (dialog.isShowing()) {
                if (mForegroundActivity == mLastActivityWithDialog.get()) {
                    MobileCenterLog.debug(LOG_TAG, "Previous dialog is still being shown in the same activity.");
                    return false;
                }

                /* Otherwise replace dialog. */
                dialog.hide();
            }
        }
        return true;
    }

    /**
     * Show dialog and remember which activity displayed it for later U.I. state change.
     *
     * @param dialog dialog.
     */
    private void showAndRememberDialogActivity(Dialog dialog) {
        dialog.show();
        mLastActivityWithDialog = new WeakReference<>(mForegroundActivity);
    }

    /**
     * Show update dialog. This can be called multiple times if clicking on HOME and app resumed
     * (it could be resumed in another activity covering the previous one).
     */
    @UiThread
    private synchronized void showUpdateDialog() {
        if (mListener == null && mUsingDefaultUpdateDialog == null) {
            mUsingDefaultUpdateDialog = true;
        }
        if (mListener != null && mForegroundActivity != mLastActivityWithDialog.get()) {
            MobileCenterLog.debug(LOG_TAG, "Calling listener.onReleaseAvailable.");
            boolean customized = mListener.onReleaseAvailable(mForegroundActivity, mReleaseDetails);
            if (customized) {
                mLastActivityWithDialog = new WeakReference<>(mForegroundActivity);
            }
            mUsingDefaultUpdateDialog = !customized;
        }
        if (mUsingDefaultUpdateDialog) {
            if (!shouldRefreshDialog(mUpdateDialog)) {
                return;
            }
            MobileCenterLog.debug(LOG_TAG, "Show default update dialog.");
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
            dialogBuilder.setTitle(R.string.mobile_center_distribute_update_dialog_title);
            final ReleaseDetails releaseDetails = mReleaseDetails;
            String message;
            if (releaseDetails.isMandatoryUpdate()) {
                message = mContext.getString(R.string.mobile_center_distribute_update_dialog_message_mandatory);
            } else {
                message = mContext.getString(R.string.mobile_center_distribute_update_dialog_message_optional);
            }
            message = formatAppNameAndVersion(message);
            dialogBuilder.setMessage(message);
            dialogBuilder.setPositiveButton(R.string.mobile_center_distribute_update_dialog_download, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    enqueueDownloadOrShowUnknownSourcesDialog(releaseDetails);
                }
            });
            dialogBuilder.setCancelable(false);
            if (!releaseDetails.isMandatoryUpdate()) {
                dialogBuilder.setNegativeButton(R.string.mobile_center_distribute_update_dialog_postpone, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        postponeRelease(releaseDetails);
                    }
                });
            }
            if (releaseDetails.getReleaseNotes() != null && releaseDetails.getReleaseNotesUrl() != null) {
                dialogBuilder.setNeutralButton(R.string.mobile_center_distribute_update_dialog_view_release_notes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewReleaseNotes(releaseDetails);
                    }
                });
            }
            mUpdateDialog = dialogBuilder.create();
            showAndRememberDialogActivity(mUpdateDialog);
        }
    }

    /**
     * View release notes. (Using top level method to be able to use whenNew in PowerMock).
     */
    private void viewReleaseNotes(ReleaseDetails releaseDetails) {
        try {
            mForegroundActivity.startActivity(new Intent(Intent.ACTION_VIEW, releaseDetails.getReleaseNotesUrl()));
        } catch (ActivityNotFoundException e) {
            MobileCenterLog.error(LOG_TAG, "Failed to navigate to release notes.", e);
        }
    }

    /**
     * Show unknown sources dialog. This can be called multiple times if clicking on HOME and app resumed
     * (it could be resumed in another activity covering the previous one).
     */
    @UiThread
    private synchronized void showUnknownSourcesDialog() {

        /* Check if we need to replace dialog. */
        if (!shouldRefreshDialog(mUnknownSourcesDialog)) {
            return;
        }
        MobileCenterLog.debug(LOG_TAG, "Show new unknown sources dialog.");

        /*
         * We invite user to go to setting and will navigate to setting upon clicking,
         * but no monitoring is possible and application will be left.
         * We want consistent behavior whether application is killed in the mean time or not.
         * Not changing any state here provide that consistency as a new update dialog
         * will be shown when coming back to application.
         *
         * Also for buttons and texts we try do to the same as the system dialog on standard devices.
         */
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
        dialogBuilder.setMessage(R.string.mobile_center_distribute_unknown_sources_dialog_message);
        final ReleaseDetails releaseDetails = mReleaseDetails;
        if (releaseDetails.isMandatoryUpdate()) {
            dialogBuilder.setCancelable(false);
        } else {
            dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

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
        }

        /* We use generic OK button as we can't promise we can navigate to settings. */
        dialogBuilder.setPositiveButton(R.string.mobile_center_distribute_unknown_sources_dialog_settings, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                goToSettings(releaseDetails);
            }
        });
        mUnknownSourcesDialog = dialogBuilder.create();
        showAndRememberDialogActivity(mUnknownSourcesDialog);
    }

    /**
     * Navigate to secure settings.
     *
     * @param releaseDetails release details to check for state change.
     */
    private synchronized void goToSettings(ReleaseDetails releaseDetails) {
        try {

            /*
             * We can't use startActivityForResult as we don't subclass activities.
             * And a no U.I. activity of our own must finish in onCreate,
             * so it cannot receive a result.
             */
            mForegroundActivity.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        } catch (ActivityNotFoundException e) {

            /* On some devices, it's not possible, user will do it by himself. */
            MobileCenterLog.warn(LOG_TAG, "No way to navigate to secure settings on this device automatically");

            /* Don't pop dialog until app restarted in that case. */
            if (releaseDetails == mReleaseDetails) {
                completeWorkflow();
            }
        }
    }

    /**
     * "Ask me in a day" postpone action.
     *
     * @param releaseDetails release details.
     */
    private synchronized void postponeRelease(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            MobileCenterLog.debug(LOG_TAG, "Postpone updates for a day.");
            PreferencesStorage.putLong(PREFERENCE_KEY_POSTPONE_TIME, System.currentTimeMillis());
            completeWorkflow();
        } else {
            showDisabledToast();
        }
    }

    /**
     * Check state did not change and schedule download of the release.
     *
     * @param releaseDetails release details.
     */
    synchronized void enqueueDownloadOrShowUnknownSourcesDialog(final ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            if (InstallerUtils.isUnknownSourcesEnabled(mContext)) {
                MobileCenterLog.debug(LOG_TAG, "Schedule download...");
                if (releaseDetails.isMandatoryUpdate()) {
                    showDownloadProgress();
                }
                mCheckedDownload = true;
                mDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadTask(mContext, releaseDetails));

                /*
                 * If we restored a cached dialog, we also started a new check release call.
                 * We might have time to click on download before the call completes (easy to
                 * reproduce with network down).
                 * In that case the download will start and we'll see a new update dialog if we
                 * don't cancel the call.
                 */
                if (mCheckReleaseApiCall != null) {
                    mCheckReleaseApiCall.cancel();
                }
            } else {
                showUnknownSourcesDialog();
            }
        } else {
            showDisabledToast();
        }
    }

    /**
     * Show disabled toast so that user is not surprised why the dialog action does not work.
     * Calling setEnabled(false) before actioning dialog is a corner case
     * (possible only if developer has code running the disable in background/or in the mean time)
     * that will likely never happen but we guard for it.
     */
    private void showDisabledToast() {
        Toast.makeText(mContext, R.string.mobile_center_distribute_dialog_actioned_on_disabled_toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * Persist download state.
     *
     * @param downloadManager download manager.
     * @param task            current task to check state change.
     * @param downloadId      download identifier.
     * @param enqueueTime     time just before enqueuing download.
     */
    @WorkerThread
    synchronized void storeDownloadRequestId(DownloadManager downloadManager, DownloadTask task, long downloadId, long enqueueTime) {

        /* Check for if state changed and task not canceled in time. */
        if (mDownloadTask == task) {

            /* Delete previous download. */
            long previousDownloadId = DistributeUtils.getStoredDownloadId();
            if (previousDownloadId >= 0) {
                MobileCenterLog.debug(LOG_TAG, "Delete previous download id=" + previousDownloadId);
                downloadManager.remove(previousDownloadId);
            }

            /* Store new download identifier. */
            PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadId);
            PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_ENQUEUED);
            PreferencesStorage.putLong(PREFERENCE_KEY_DOWNLOAD_TIME, enqueueTime);

            /* Start monitoring progress for mandatory update. */
            if (mReleaseDetails.isMandatoryUpdate()) {
                checkDownload(mContext, downloadId, true);
            }
        } else {

            /* State changed quickly, cancel download. */
            MobileCenterLog.debug(LOG_TAG, "State changed while downloading, cancel id=" + downloadId);
            downloadManager.remove(downloadId);
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
     * Check a download state and take action depending on that state.
     *
     * @param context       any application context.
     * @param downloadId    download identifier from DownloadManager.
     * @param checkProgress true to only check progress, false to also process install if done.
     */
    synchronized void checkDownload(@NonNull Context context, long downloadId, boolean checkProgress) {

        /* Querying download manager and even the start intent are detected by strict mode so we do that in background. */
        mCheckDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new CheckDownloadTask(context, downloadId, checkProgress, mReleaseDetails));
    }

    /**
     * Post notification about a completed download if we are in background when download completes.
     * If this method is called on app process restart or if application is in foreground
     * when download completes, it will not notify and return that the install U.I. should be shown now.
     *
     * @param releaseDetails release details to check state.
     * @param intent         prepared install intent.
     * @return false if install U.I should be shown now, true if a notification was posted or if the task was canceled.
     */
    synchronized boolean notifyDownload(ReleaseDetails releaseDetails, Intent intent) {

        /* Check state. */
        if (releaseDetails != mReleaseDetails) {
            return true;
        }

        /*
         * If we already notified, that means this check was triggered by application being resumed,
         * thus in foreground at the moment the check download async task was started.
         *
         * We should not hold the install any longer now, even if the async task was long enough
         * for app to be in background again, we should show install U.I. now.
         */
        if (mForegroundActivity != null || getStoredDownloadState() == DOWNLOAD_STATE_NOTIFIED) {
            return false;
        }

        /* Post notification. */
        MobileCenterLog.debug(LOG_TAG, "Post a notification as the download finished in background.");
        Notification.Builder builder = new Notification.Builder(mContext)
                .setTicker(mContext.getString(R.string.mobile_center_distribute_install_ready_title))
                .setContentTitle(mContext.getString(R.string.mobile_center_distribute_install_ready_title))
                .setContentText(getInstallReadyMessage())
                .setSmallIcon(mContext.getApplicationInfo().icon)
                .setContentIntent(PendingIntent.getActivities(mContext, 0, new Intent[]{intent}, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setStyle(new Notification.BigTextStyle().bigText(getInstallReadyMessage()));
        }
        Notification notification = DistributeUtils.buildNotification(builder);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(DistributeUtils.getNotificationId(), notification);
        PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_NOTIFIED);

        /* Reset check download flag to show install U.I. on resume if notification ignored. */
        mCheckedDownload = false;
        return true;
    }

    /**
     * Used to avoid querying download manager on every activity change.
     *
     * @param releaseDetails release details to check state.
     */
    synchronized void markDownloadStillInProgress(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            MobileCenterLog.verbose(LOG_TAG, "Download is still in progress...");
            mCheckedDownload = true;
        }
    }

    /**
     * Remove a previously downloaded file and any notification.
     */
    @SuppressLint("VisibleForTests")
    private synchronized void removeDownload(long downloadId) {
        cancelNotification();
        AsyncTaskUtils.execute(LOG_TAG, new RemoveDownloadTask(mContext, downloadId));
    }

    /**
     * Show download progress.
     */
    private void showDownloadProgress() {
        mProgressDialog = new ProgressDialog(mForegroundActivity);
        mProgressDialog.setTitle(R.string.mobile_center_distribute_downloading_mandatory_update);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressNumberFormat(null);
        mProgressDialog.setProgressPercentFormat(null);
        showAndRememberDialogActivity(mProgressDialog);
    }

    /**
     * Hide progress dialog and stop updating.
     */
    private synchronized void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.hide();
            mProgressDialog = null;
            HandlerUtils.getMainHandler().removeCallbacksAndMessages(HANDLER_TOKEN_CHECK_PROGRESS);
        }
    }

    /**
     * Update progress dialog for mandatory update.
     */
    synchronized void updateProgressDialog(ReleaseDetails releaseDetails, DownloadProgress downloadProgress) {

        /* If not canceled and U.I. context did not change. */
        if (releaseDetails == mReleaseDetails && mProgressDialog != null) {

            /* If file size is known update downloadProgress bar. */
            if (downloadProgress.getTotalSize() >= 0) {

                /* When we switch from indeterminate to determinate */
                if (mProgressDialog.isIndeterminate()) {

                    /* Configure the progress dialog determinate style. */
                    mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                    mProgressDialog.setProgressNumberFormat(mForegroundActivity.getString(R.string.mobile_center_distribute_download_progress_number_format));
                    mProgressDialog.setIndeterminate(false);
                    mProgressDialog.setMax((int) (downloadProgress.getTotalSize() / MEBIBYTE_IN_BYTES));
                }
                mProgressDialog.setProgress((int) (downloadProgress.getCurrentSize() / MEBIBYTE_IN_BYTES));
            }

            /* And schedule the next check. */
            HandlerUtils.getMainHandler().postAtTime(new Runnable() {

                @Override
                public void run() {
                    checkDownload(mContext, DistributeUtils.getStoredDownloadId(), true);
                }
            }, HANDLER_TOKEN_CHECK_PROGRESS, SystemClock.uptimeMillis() + CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS);
        }
    }

    /**
     * Show modal dialog with install button if mandatory update ready and user cancelled install.
     */
    private synchronized void showMandatoryDownloadReadyDialog() {
        if (shouldRefreshDialog(mCompletedDownloadDialog)) {
            final ReleaseDetails releaseDetails = mReleaseDetails;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
            dialogBuilder.setCancelable(false);
            dialogBuilder.setTitle(R.string.mobile_center_distribute_install_ready_title);
            dialogBuilder.setMessage(getInstallReadyMessage());
            dialogBuilder.setPositiveButton(R.string.mobile_center_distribute_install, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    installMandatoryUpdate(releaseDetails);
                }
            });
            mCompletedDownloadDialog = dialogBuilder.create();
            showAndRememberDialogActivity(mCompletedDownloadDialog);
        }
    }

    /**
     * Get text for app is ready to be installed.
     */
    private String getInstallReadyMessage() {
        return formatAppNameAndVersion(mContext.getString(R.string.mobile_center_distribute_install_ready_message));
    }

    /**
     * Inject app name version and version code in a format string.
     */
    private String formatAppNameAndVersion(String format) {
        return String.format(format, mContext.getString(mContext.getApplicationInfo().labelRes), mReleaseDetails.getShortVersion(), mReleaseDetails.getVersion());
    }

    /**
     * Install mandatory update after clicking on the install dialog button.
     *
     * @param releaseDetails release details.
     */
    private synchronized void installMandatoryUpdate(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            checkDownload(mContext, DistributeUtils.getStoredDownloadId(), false);
        } else {
            showDisabledToast();
        }
    }

    /**
     * Update download state to installing if state did not change.
     *
     * @param releaseDetails to check state change.
     */
    synchronized void setInstalling(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            cancelNotification();
            PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_INSTALLING);
        }
    }
}
