/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import android.text.TextUtils;
import android.widget.Toast;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.distribute.channel.DistributeInfoTracker;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.distribute.download.ReleaseDownloaderFactory;
import com.microsoft.appcenter.distribute.ingestion.DistributeIngestion;
import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.distribute.ingestion.models.json.DistributionStartSessionLogFactory;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.context.SessionContext;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DEFAULT_API_URL;
import static com.microsoft.appcenter.distribute.DistributeConstants.DEFAULT_INSTALL_URL;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_AVAILABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_INSTALLING;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_NOTIFIED;
import static com.microsoft.appcenter.distribute.DistributeConstants.GET_LATEST_PRIVATE_RELEASE_PATH_FORMAT;
import static com.microsoft.appcenter.distribute.DistributeConstants.GET_LATEST_PUBLIC_RELEASE_PATH_FORMAT;
import static com.microsoft.appcenter.distribute.DistributeConstants.HEADER_API_TOKEN;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.NOTIFICATION_CHANNEL_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_INSTALL_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_UPDATE_SETUP_FAILED;
import static com.microsoft.appcenter.distribute.DistributeConstants.POSTPONE_TIME_THRESHOLD;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_POSTPONE_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.appcenter.distribute.DistributeConstants.SERVICE_NAME;
import static com.microsoft.appcenter.distribute.DistributeUtils.computeReleaseHash;
import static com.microsoft.appcenter.distribute.DistributeUtils.getStoredDownloadState;

/**
 * Distribute service.
 */
public class Distribute extends AbstractAppCenterService {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Distribute sInstance;

    /**
     * Log factories managed by this service.
     */
    private final Map<String, LogFactory> mFactories;

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
     * Remember if we already tried to open the tester app to update setup.
     */
    private boolean mTesterAppOpenedOrAborted;

    /**
     * Remember if we already tried to open the browser to update setup.
     */
    private boolean mBrowserOpenedOrAborted;

    /**
     * In memory request identifier if we receive deep link intent before onStart.
     */
    private String mBeforeStartRequestId;

    /**
     * In memory distribution group identifier if we receive deep link intent before onStart.
     */
    private String mBeforeStartDistributionGroupId;

    /**
     * In memory token if we receive deep link intent before onStart.
     */
    private String mBeforeStartUpdateToken;

    /**
     * In memory update setup failure error message if we receive deep link intent before onStart.
     */
    private String mBeforeStartUpdateSetupFailed;

    /**
     * In memory tester app update setup failure error message if we receive deep link intent before onStart.
     */
    private String mBeforeStartTesterAppUpdateSetupFailed;

    /**
     * Update track as set and returned by the API. Can only be set before start.
     */
    private int mUpdateTrack = UpdateTrack.PUBLIC;

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
     * Alert system dialog that was shown.
     */
    private Dialog mAlertSystemWindowsDialog;

    /**
     * Mandatory download completed in app notification.
     */
    private Dialog mCompletedDownloadDialog;

    /**
     * Update setup failed dialog.
     */
    private Dialog mUpdateSetupFailedDialog;

    /**
     * Last activity that did show a dialog.
     * Used to avoid replacing a dialog in same screen as it causes flickering.
     */
    private WeakReference<Activity> mLastActivityWithDialog = new WeakReference<>(null);

    /**
     * Release downloader. It can use {@link DownloadManager} or direct HTTPS downloading.
     */
    private ReleaseDownloader mReleaseDownloader;

    /**
     * Download release listener.
     */
    private ReleaseDownloadListener mReleaseDownloaderListener;

    /**
     * Install release listener.
     */
    private ReleaseInstallerListener mReleaseInstallerListener;

    /**
     * Receiver of installing a new release.
     */
    private AppCenterPackageInstallerReceiver mAppCenterPackageInstallerReceiver;

    /**
     * Intent filter of receiver for a new release.
     */
    private IntentFilter mPackageInstallerReceiverFilter;

    /**
     * Remember if we checked download since our own process restarted.
     */
    private boolean mCheckedDownload;

    /**
     * Remember whether the installation of a new release is started.
     */
    private boolean mInstallInProgress;

    /**
     * True when distribute workflow reached final state.
     * This can be reset to check update again when app restarts.
     */
    private boolean mWorkflowCompleted;

    /**
     * Channel listener which adds extra fields to logs.
     */
    private DistributeInfoTracker mDistributeInfoTracker;

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
     * Flag to track whether the distribute feature can be used in a debuggable build.
     * Flag is false by default.
     * Updated by calling {@link #setEnabledForDebuggableBuild(boolean)}.
     */
    private boolean mEnabledForDebuggableBuild;

    /**
     * Flag to check if automatic check for update is disabled.
     */
    private boolean mAutomaticCheckForUpdateDisabled;

    /**
     * Flag to check if manual check for update was requested.
     */
    private boolean mManualCheckForUpdateRequested;

    /**
     * Init.
     */
    private Distribute() {
        mFactories = new HashMap<>();
        mFactories.put(DistributionStartSessionLog.TYPE, new DistributionStartSessionLogFactory());
        mAppCenterPackageInstallerReceiver = new AppCenterPackageInstallerReceiver();
        mPackageInstallerReceiverFilter = new IntentFilter();
        mPackageInstallerReceiverFilter.addAction(AppCenterPackageInstallerReceiver.START_ACTION);
        mPackageInstallerReceiverFilter.addAction(AppCenterPackageInstallerReceiver.MY_PACKAGE_REPLACED_ACTION);
    }

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
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
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Distribute service.
     * <p>
     * The state is persisted in the device's storage across application launches.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
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
     * Add stores allowed to perform in-app updates.
     *
     * @param stores list of stores allowed to perform in-app updates.
     */
    public static void addStores(Set<String> stores) {
        InstallerUtils.addLocalStores(stores);
    }

    /**
     * Get the current update track (public vs private).
     */
    // TODO Remove suppress when app uses it without reflection on jCenter
    @SuppressWarnings("WeakerAccess")
    public static int getUpdateTrack() {
        return getInstance().getInstanceUpdateTrack();
    }

    /**
     * Set the update track (public vs private).
     *
     * @param updateTrack update track.
     */
    public static void setUpdateTrack(@UpdateTrack int updateTrack) {
        getInstance().setInstanceUpdateTrack(updateTrack);
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
     * Set whether the distribute service can be used within a debuggable build.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    public static void setEnabledForDebuggableBuild(boolean enabled) {
        getInstance().setInstanceEnabledForDebuggableBuild(enabled);
    }

    /**
     * If update dialog is customized by returning <code>true</code> in  {@link DistributeListener#onReleaseAvailable(Activity, ReleaseDetails)},
     * You need to tell the distribute SDK using this function what is the user action.
     *
     * @param updateAction one of {@link UpdateAction} actions.
     *                     For mandatory updates, only {@link UpdateAction#UPDATE} is allowed.
     */
    public static void notifyUpdateAction(@UpdateAction int updateAction) {
        getInstance().handleUpdateAction(updateAction);
    }

    /**
     * Trigger a check for update.
     * If the application is in background, it will delay the check for update until the application is in foreground.
     * This call has no effect if there is already an ongoing check.
     */
    public static void checkForUpdate() {
        getInstance().instanceCheckForUpdate();
    }

    /**
     * Disable automatic check for update before the service starts.
     */
    public static void disableAutomaticCheckForUpdate() {
        getInstance().instanceDisableAutomaticCheckForUpdate();
    }

    /**
     * Implements {@link #disableAutomaticCheckForUpdate()}.
     */
    private synchronized void instanceDisableAutomaticCheckForUpdate() {
        if (mChannel != null) {
            AppCenterLog.error(LOG_TAG, "Automatic check for update cannot be disabled after Distribute is started.");
            return;
        }
        mAutomaticCheckForUpdateDisabled = true;
    }

    @Override
    protected String getGroupName() {
        return DISTRIBUTE_GROUP;
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
    protected int getTriggerCount() {
        return 1;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    /**
     * Constant marking event of the distribute group.
     */
    private static final String DISTRIBUTE_GROUP = "group_distribute";

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        mAppSecret = appSecret;
        try {
            mPackageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            AppCenterLog.error(LOG_TAG, "Could not get self package info.", e);
        }

        /*
         * Apply enabled state is called by this method, we need fields to be initialized before.
         * So call super method at the end.
         */
        super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);
    }

    /**
     * Set context, used when need to manipulate context before onStarted.
     * For example when download completes after application process exited.
     */
    @WorkerThread
    synchronized void startFromBackground(Context context) {
        if (mAppSecret == null) {
            AppCenterLog.debug(LOG_TAG, "Called before onStart, init storage");
            mContext = context;
            SharedPreferencesManager.initialize(mContext);
            updateReleaseDetails(DistributeUtils.loadCachedReleaseDetails());
        }
    }

    /**
     * Reset current workflow to allow a new update check if we are not already in the process
     * of checking one.
     *
     * @return true if workflow was reset, false otherwise.
     */
    private boolean tryResetWorkflow() {
        if (getStoredDownloadState() == DOWNLOAD_STATE_COMPLETED && mCheckReleaseCallId == null) {
            mWorkflowCompleted = false;
            mBrowserOpenedOrAborted = false;
            return true;
        }
        return false;
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mForegroundActivity = activity;

        /* If started, resume now, otherwise this will be called by onStarted. */
        if (mChannel != null) {
            resumeDistributeWorkflow();
        }
        registerReceiver();
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        mForegroundActivity = null;

        /* Hide mandatory update progress dialog if exists. */
        if (mReleaseDownloaderListener != null) {
            mReleaseDownloaderListener.hideProgressDialog();
        }
    }

    @Override
    public void onApplicationEnterForeground() {
        if (mChannel != null) {
            AppCenterLog.debug(LOG_TAG, "Resetting workflow on entering foreground.");
            tryResetWorkflow();
        }
    }

    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
            changeDistributionGroupIdAfterAppUpdateIfNeeded();

            /* Enable the distribute info tracker. */
            String distributionGroupId = SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID);
            mDistributeInfoTracker = new DistributeInfoTracker(distributionGroupId);
            mChannel.addListener(mDistributeInfoTracker);

            /* Resume distribute workflow only if there is foreground activity. */
            resumeWorkflowIfForeground();

            /* Register package installer receiver. */
            registerReceiver();
        } else {

            /* Clean all state on disabling, cancel everything. Keep only redirection parameters. */
            mTesterAppOpenedOrAborted = false;
            mBrowserOpenedOrAborted = false;
            mWorkflowCompleted = false;
            cancelPreviousTasks();
            SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
            SharedPreferencesManager.remove(PREFERENCE_KEY_POSTPONE_TIME);
            SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
            SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
            SharedPreferencesManager.remove(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY);

            /* Disable the distribute info tracker. */
            mChannel.removeListener(mDistributeInfoTracker);
            mDistributeInfoTracker = null;

            /* Unregister package installer receiver. */
            unregisterReceiver();
        }
    }

    /**
     * Register package installer receiver.
     */
    private synchronized void registerReceiver() {
        if (mForegroundActivity != null) {
            try {
                mForegroundActivity.registerReceiver(mAppCenterPackageInstallerReceiver, mPackageInstallerReceiverFilter);
                AppCenterLog.debug(LOG_TAG, "The receiver for installing a new release was registered.");
            } catch (IllegalArgumentException e) {
                AppCenterLog.error(LOG_TAG, "The receiver wasn't registered.", e);
            }
        } else {
            AppCenterLog.warn(LOG_TAG, "Couldn't register receiver due to activity is null.");
        }
    }

    /**
     * Unregister package installer receiver.
     */
    private synchronized void unregisterReceiver() {
        if (mForegroundActivity != null) {
            try {
                mForegroundActivity.unregisterReceiver(mAppCenterPackageInstallerReceiver);
                AppCenterLog.debug(LOG_TAG, "The receiver for installing a new release was unregistered.");
            } catch (IllegalArgumentException e) {
                AppCenterLog.error(LOG_TAG, "The receiver wasn't unregistered.", e);
            }
        } else {
            AppCenterLog.warn(LOG_TAG, "Couldn't register unregister due to activity is null.");
        }
    }

    @WorkerThread
    private void resumeWorkflowIfForeground() {
        if (mForegroundActivity != null) {
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    resumeDistributeWorkflow();
                }
            });
        } else {
            AppCenterLog.debug(LOG_TAG, "Distribute workflow will be resumed on activity resume event.");
        }
    }

    /**
     * Implements {@link #notifyUpdateAction(int)}.
     */
    @VisibleForTesting
    synchronized void handleUpdateAction(final int updateAction) {

        /*
         * We need to check if it is enabled and we also need to run download code in U.I. thread
         * so post the command using the async method to achieve both goals at once.
         */
        isInstanceEnabledAsync().thenAccept(new AppCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean enabled) {
                if (!enabled) {
                    AppCenterLog.error(LOG_TAG, "Distribute is disabled");
                    return;
                }
                boolean isDownloading = mReleaseDownloader != null && mReleaseDownloader.isDownloading();
                if (getStoredDownloadState() != DOWNLOAD_STATE_AVAILABLE || isDownloading) {
                    AppCenterLog.error(LOG_TAG, "Cannot handle user update action at this time.");
                    return;
                }
                if (mUsingDefaultUpdateDialog) {
                    AppCenterLog.error(LOG_TAG, "Cannot handle user update action when using default dialog.");
                    return;
                }
                switch (updateAction) {

                    case UpdateAction.UPDATE:
                        enqueueDownloadOrShowUnknownSourcesDialog(mReleaseDetails);
                        break;

                    case UpdateAction.POSTPONE:
                        if (mReleaseDetails.isMandatoryUpdate()) {
                            AppCenterLog.error(LOG_TAG, "Cannot postpone a mandatory update.");
                            return;
                        }
                        postponeRelease(mReleaseDetails);
                        break;

                    default:
                        AppCenterLog.error(LOG_TAG, "Invalid update action: " + updateAction);
                }
            }
        });
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
     * Implements {@link #getUpdateTrack()}.
     */
    private synchronized int getInstanceUpdateTrack() {
        return mUpdateTrack;
    }

    /**
     * Implements {@link #setUpdateTrack(int)}.
     */
    private synchronized void setInstanceUpdateTrack(final int updateTrack) {
        if (mContext != null) {
            AppCenterLog.error(LOG_TAG, "Update track cannot be set after Distribute is started.");
            return;
        }
        if (DistributeUtils.isInvalidUpdateTrack(updateTrack)) {
            AppCenterLog.error(LOG_TAG, "Invalid argument passed to Distribute.setUpdateTrack().");
            return;
        }
        mUpdateTrack = updateTrack;
    }

    /**
     * Implements {@link #setListener(DistributeListener)}.
     */
    private synchronized void setInstanceListener(DistributeListener listener) {
        mListener = listener;
    }

    /**
     * Implements {@link #setEnabledForDebuggableBuild(boolean)}.
     */
    private synchronized void setInstanceEnabledForDebuggableBuild(boolean enabled) {
        mEnabledForDebuggableBuild = enabled;
    }

    /**
     * Implements {@link #checkForUpdate()}.
     */
    private void instanceCheckForUpdate() {
        post(new Runnable() {

            @Override
            public void run() {
                handleCheckForUpdate();
            }
        });
    }

    @WorkerThread
    private synchronized void handleCheckForUpdate() {
        mManualCheckForUpdateRequested = true;
        if (tryResetWorkflow()) {
            resumeWorkflowIfForeground();
        } else {
            AppCenterLog.info(LOG_TAG, "A check for update is already ongoing.");
        }
    }

    /**
     * Cancel everything.
     */
    private synchronized void cancelPreviousTasks() {
        if (mCheckReleaseApiCall != null) {
            mCheckReleaseApiCall.cancel();
            mCheckReleaseApiCall = null;
        }
        mCheckReleaseCallId = null;
        mUpdateDialog = null;
        mUnknownSourcesDialog = null;
        mAlertSystemWindowsDialog = null;
        mCompletedDownloadDialog = null;
        mUpdateSetupFailedDialog = null;
        mLastActivityWithDialog.clear();
        mUsingDefaultUpdateDialog = null;
        mCheckedDownload = false;
        mInstallInProgress = false;
        mManualCheckForUpdateRequested = false;
        updateReleaseDetails(null);
        SharedPreferencesManager.remove(PREFERENCE_KEY_RELEASE_DETAILS);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_TIME);
    }

    /**
     * Method that triggers the distribute workflow or proceed to the next step.
     */
    @UiThread
    private synchronized void resumeDistributeWorkflow() {
        AppCenterLog.debug(LOG_TAG, "Resume distribute workflow...");
        if (mPackageInfo != null && mForegroundActivity != null && !mWorkflowCompleted && isInstanceEnabled()) {

            /* Don't go any further it this is a debug app. */
            if ((mContext.getApplicationInfo().flags & FLAG_DEBUGGABLE) == FLAG_DEBUGGABLE && !mEnabledForDebuggableBuild) {
                AppCenterLog.info(LOG_TAG, "Not checking for in-app updates in debuggable build.");
                mWorkflowCompleted = true;
                mManualCheckForUpdateRequested = false;
                return;
            }

            /* Don't go any further if the app was installed from an app store. */
            if (InstallerUtils.isInstalledFromAppStore(LOG_TAG, mContext)) {
                AppCenterLog.info(LOG_TAG, "Not checking in app updates as installed from a store.");
                mWorkflowCompleted = true;
                mManualCheckForUpdateRequested = false;
                return;
            }

            /* Continue to install a new release if before resume was shown dialog. */
            if (mAlertSystemWindowsDialog != null) {
                mAlertSystemWindowsDialog.dismiss();
                mAlertSystemWindowsDialog = null;
                installingUpdate();
                return;
            }

            /* Do nothing during installing a new release. */
            if (mInstallInProgress) {
                if (mReleaseInstallerListener != null) {
                    mReleaseInstallerListener.showInstallProgressDialog(mForegroundActivity);
                }
                AppCenterLog.info(LOG_TAG, "Installing in progress...");
                return;
            }

            /*
             * If failed to enable in-app updates on the same app build before, don't go any further.
             * Only if the app build is different (different package hash), try enabling in-app updates again.
             * This applies to private track only.
             */
            boolean isPublicTrack = mUpdateTrack == UpdateTrack.PUBLIC;
            if (!isPublicTrack) {
                String updateSetupFailedPackageHash = SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
                if (updateSetupFailedPackageHash != null) {
                    String releaseHash = DistributeUtils.computeReleaseHash(this.mPackageInfo);
                    if (releaseHash.equals(updateSetupFailedPackageHash)) {
                        AppCenterLog.info(LOG_TAG, "Skipping in-app updates setup, because it already failed on this release before.");
                        return;
                    } else {
                        AppCenterLog.info(LOG_TAG, "Re-attempting in-app updates setup and cleaning up failure info from storage.");
                        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
                        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
                        SharedPreferencesManager.remove(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY);
                    }
                }
            }

            /* If we received the redirection parameters before App Center was started/enabled, process them now. */
            if (mBeforeStartRequestId != null) {
                AppCenterLog.debug(LOG_TAG, "Processing redirection parameters we kept in memory before onStarted");
                if (mBeforeStartDistributionGroupId != null) {
                    storeRedirectionParameters(mBeforeStartRequestId, mBeforeStartDistributionGroupId, mBeforeStartUpdateToken);
                } else if (mBeforeStartUpdateSetupFailed != null) {
                    storeUpdateSetupFailedParameter(mBeforeStartRequestId, mBeforeStartUpdateSetupFailed);
                }
                if (mBeforeStartTesterAppUpdateSetupFailed != null) {
                    storeTesterAppUpdateSetupFailedParameter(mBeforeStartRequestId, mBeforeStartTesterAppUpdateSetupFailed);
                }
                mBeforeStartRequestId = null;
                mBeforeStartDistributionGroupId = null;
                mBeforeStartUpdateToken = null;
                mBeforeStartUpdateSetupFailed = null;
                mBeforeStartTesterAppUpdateSetupFailed = null;
                return;
            }

            /* Load cached release details if process restarted and we have such a cache. */
            int downloadState = getStoredDownloadState();
            if (mReleaseDetails == null && downloadState != DOWNLOAD_STATE_COMPLETED) {
                updateReleaseDetails(DistributeUtils.loadCachedReleaseDetails());

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
                if (mPackageInfo.lastUpdateTime > SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_TIME)) {
                    AppCenterLog.debug(LOG_TAG, "Discarding previous download as application updated.");
                    cancelPreviousTasks();
                }

                /* Otherwise check currently processed release. */
                else {

                    /*
                     * If app restarted, try to resume (or restart if not available) download.
                     * Install UI will be shown by listener once download will be completed.
                     */
                    mCheckedDownload = true;
                    resumeDownload();

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

                    /* Resume (or restart if not available) download. */
                    resumeDownload();

                    /* Refresh mandatory dialog progress or do nothing otherwise. */
                    showDownloadProgress();
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

                /*
                 * Or restore update dialog if that's the last thing we did before being paused.
                 * Also checking we are not about to download (DownloadTask might still be running and thus not enqueued yet).
                 */
                else if (mReleaseDownloader == null || !mReleaseDownloader.isDownloading()) {
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

            /*
             * If the in-app updates setup failed, and user ignores the failure, store the error
             * message and also store the package hash that the failure occurred on. The setup
             * will only be re-attempted the next time the app gets updated (and package hash changes).
             */
            String updateSetupFailedMessage = SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
            if (updateSetupFailedMessage != null) {
                AppCenterLog.debug(LOG_TAG, "In-app updates setup failure detected.");
                showUpdateSetupFailedDialog();
                return;
            }

            /* Nothing more to do for now if we are already calling API to check release. */
            if (mCheckReleaseCallId != null) {
                AppCenterLog.verbose(LOG_TAG, "Already checking or checked latest release.");
                return;
            }

            /* Do not proceed if automatic check for update is disabled and manual check for update has not been called. */
            if (mAutomaticCheckForUpdateDisabled && !mManualCheckForUpdateRequested) {
                AppCenterLog.debug(LOG_TAG, "Automatic check for update is disabled. The SDK will not check for update now.");
                return;
            }

            /*
             * Check if we have previously stored the redirection parameters from private group or we simply use public track.
             */
            String updateToken = SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN);
            String distributionGroupId = SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID);
            if (isPublicTrack || updateToken != null) {

                /* We have what we need to check for updates via API. */
                decryptAndGetReleaseDetails(isPublicTrack ? null : updateToken, distributionGroupId);
                return;
            }

            /* If not, open native app (if installed) to update setup, unless it already failed. Otherwise, use the browser. */
            String testerAppUpdateSetupFailedMessage = SharedPreferencesManager.getString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY);
            boolean shouldUseTesterAppForUpdateSetup = isAppCenterTesterAppInstalled() && TextUtils.isEmpty(testerAppUpdateSetupFailedMessage) && !mContext.getPackageName().equals(DistributeUtils.TESTER_APP_PACKAGE_NAME);
            if (shouldUseTesterAppForUpdateSetup && !mTesterAppOpenedOrAborted) {
                DistributeUtils.updateSetupUsingTesterApp(mForegroundActivity, mPackageInfo);
                mTesterAppOpenedOrAborted = true;
            } else if (!mBrowserOpenedOrAborted) {
                DistributeUtils.updateSetupUsingBrowser(mForegroundActivity, mInstallUrl, mAppSecret, mPackageInfo);
                mBrowserOpenedOrAborted = true;
            }
        }
    }

    private boolean isAppCenterTesterAppInstalled() {
        try {
            mContext.getPackageManager().getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
        return true;
    }

    private void decryptAndGetReleaseDetails(String updateToken, String distributionGroupId) {

        /* Decrypt token if any. */
        if (updateToken != null) {
            CryptoUtils.DecryptedData decryptedData = CryptoUtils.getInstance(mContext).decrypt(updateToken);
            String newEncryptedData = decryptedData.getNewEncryptedData();

            /* Store new encrypted value if updated. */
            if (newEncryptedData != null) {
                SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, newEncryptedData);
            }
            updateToken = decryptedData.getDecryptedData();
        }

        /* Check latest release. */
        getLatestReleaseDetails(distributionGroupId, updateToken);
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
            AppCenterLog.debug(LOG_TAG, "Delete notification");
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            //noinspection ConstantConditions
            notificationManager.cancel(DistributeUtils.getNotificationId());
        }
    }

    /**
     * Reset all variables that matter to restart checking a new release on launcher activity restart.
     */
    synchronized void completeWorkflow() {
        cancelNotification();
        SharedPreferencesManager.remove(PREFERENCE_KEY_RELEASE_DETAILS);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        mCheckReleaseApiCall = null;
        mCheckReleaseCallId = null;
        mUpdateDialog = null;
        mUpdateSetupFailedDialog = null;
        mUnknownSourcesDialog = null;
        mAlertSystemWindowsDialog = null;
        mLastActivityWithDialog.clear();
        mReleaseDetails = null;
        if (mReleaseDownloaderListener != null) {
            mReleaseDownloaderListener.hideProgressDialog();
        }
        if (mReleaseInstallerListener != null) {
            mReleaseInstallerListener.hideInstallProgressDialog();
        }
        mWorkflowCompleted = true;
        mManualCheckForUpdateRequested = false;
    }

    /**
     * Store update setup failure message used later to show in setup failure dialog for user.
     */
    synchronized void storeUpdateSetupFailedParameter(@NonNull String requestId, @NonNull String updateSetupFailed) {

        /* Keep redirection parameters for later if we are not started and enabled yet. */
        if (mContext == null) {
            AppCenterLog.debug(LOG_TAG, "Update setup failed parameter received before onStart, keep it in memory.");
            mBeforeStartRequestId = requestId;
            mBeforeStartUpdateSetupFailed = updateSetupFailed;
        } else if (requestId.equals(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID))) {
            AppCenterLog.debug(LOG_TAG, "Stored update setup failed parameter.");
            SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY, updateSetupFailed);
        } else {
            AppCenterLog.warn(LOG_TAG, "Ignoring redirection parameters as requestId is invalid.");
        }
    }

    /**
     * Store a flag for failure to enable updates from the tester apps, to later reattempt using the browser update setup.
     */
    synchronized void storeTesterAppUpdateSetupFailedParameter(@NonNull String requestId, @NonNull String testerAppUpdateSetupFailed) {

        /* Keep redirection parameters for later if we are not started and enabled yet. */
        if (mContext == null) {
            AppCenterLog.debug(LOG_TAG, "Tester app update setup failed parameter received before onStart, keep it in memory.");
            mBeforeStartRequestId = requestId;
            mBeforeStartTesterAppUpdateSetupFailed = testerAppUpdateSetupFailed;
        } else if (requestId.equals(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID))) {
            AppCenterLog.debug(LOG_TAG, "Stored tester app update setup failed parameter.");
            SharedPreferencesManager.putString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY, testerAppUpdateSetupFailed);
        } else {
            AppCenterLog.warn(LOG_TAG, "Ignoring redirection parameters as requestId is invalid.");
        }
    }

    /**
     * Store update token and possibly trigger application update check.
     */
    synchronized void storeRedirectionParameters(@NonNull String requestId, @NonNull String distributionGroupId, String updateToken) {

        /* Keep redirection parameters for later if we are not started and enabled yet. */
        if (mContext == null) {
            AppCenterLog.debug(LOG_TAG, "Redirection parameters received before onStart, keep them in memory.");
            mBeforeStartRequestId = requestId;
            mBeforeStartUpdateToken = updateToken;
            mBeforeStartDistributionGroupId = distributionGroupId;
        } else if (requestId.equals(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID))) {
            if (updateToken != null) {
                String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(updateToken);
                SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, encryptedToken);
            } else {
                SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_TOKEN);
            }
            SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
            processDistributionGroupId(distributionGroupId);
            AppCenterLog.debug(LOG_TAG, "Stored redirection parameters.");
            cancelPreviousTasks();
            getLatestReleaseDetails(distributionGroupId, updateToken);
        } else {
            AppCenterLog.warn(LOG_TAG, "Ignoring redirection parameters as requestId is invalid.");
        }
    }

    private void processDistributionGroupId(@NonNull String distributionGroupId) {
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, distributionGroupId);
        mDistributeInfoTracker.updateDistributionGroupId(distributionGroupId);
        enqueueDistributionStartSessionLog();
    }

    /**
     * Get latest release details from server.
     *
     * @param distributionGroupId distribution group id.
     * @param updateToken         token to secure API call.
     */
    @VisibleForTesting
    synchronized void getLatestReleaseDetails(final String distributionGroupId, String updateToken) {
        AppCenterLog.debug(LOG_TAG, "Get latest release details...");
        String releaseHash = computeReleaseHash(mPackageInfo);
        String url = mApiUrl;
        if (updateToken == null) {
            url += String.format(GET_LATEST_PUBLIC_RELEASE_PATH_FORMAT, mAppSecret, releaseHash, getReportingParametersForUpdatedRelease(true, distributionGroupId));
        } else {
            url += String.format(GET_LATEST_PRIVATE_RELEASE_PATH_FORMAT, mAppSecret, releaseHash, getReportingParametersForUpdatedRelease(false, distributionGroupId));
        }
        Map<String, String> headers = new HashMap<>();
        if (updateToken != null) {
            headers.put(HEADER_API_TOKEN, updateToken);
        }
        final Object releaseCallId = mCheckReleaseCallId = new Object();
        mCheckReleaseApiCall = new DistributeIngestion(mContext).checkReleaseAsync(mAppSecret, url, headers, new ServiceCallback() {

            @Override
            public void onCallSucceeded(final HttpResponse httpResponse) {
                try {
                    String payload = httpResponse.getPayload();
                    handleApiCallSuccess(releaseCallId, payload, ReleaseDetails.parse(payload), distributionGroupId);
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

            /* Complete workflow in error. */
            completeWorkflow();

            /* Delete token on unrecoverable HTTP error. */
            if (!HttpUtils.isRecoverableError(e)) {

                /*
                 * Unless its a special case: 404 with json code that no release is found.
                 * Could happen by cleaning releases with remove button.
                 */
                if (e instanceof HttpException) {
                    HttpException httpException = (HttpException) e;
                    String code = null;
                    try {

                        /* We actually don't care of the http code if JSON code is specified. */
                        ErrorDetails errorDetails = ErrorDetails.parse(httpException.getHttpResponse().getPayload());
                        code = errorDetails.getCode();
                    } catch (JSONException je) {
                        AppCenterLog.verbose(LOG_TAG, "Cannot read the error as JSON", je);
                    }
                    if (ErrorDetails.NO_RELEASES_FOR_USER_CODE.equals(code) || ErrorDetails.NO_RELEASES_FOUND.equals(code)) {
                        AppCenterLog.info(LOG_TAG, "No release available to the current user.");
                        if (mListener != null && mForegroundActivity != null) {
                            AppCenterLog.debug(LOG_TAG, "Calling listener.onNoReleaseAvailable.");
                            mListener.onNoReleaseAvailable(mForegroundActivity);
                        }
                    } else {
                        AppCenterLog.error(LOG_TAG, "Failed to check latest release (delete setup state)", e);
                        SharedPreferencesManager.remove(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID);
                        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_TOKEN);
                        SharedPreferencesManager.remove(PREFERENCE_KEY_POSTPONE_TIME);
                        mDistributeInfoTracker.removeDistributionGroupId();
                    }
                }

                /*
                 * Non HTTP errors: just no retry but keep token for next launch,
                 * it could be SSL error due to WIFI sign-in for example.
                 */
                else {
                    AppCenterLog.error(LOG_TAG, "Failed to check latest release", e);
                }
            }
        }
    }

    /**
     * Handle API call success.
     */
    private synchronized void handleApiCallSuccess(Object releaseCallId, String rawReleaseDetails, @NonNull ReleaseDetails releaseDetails, String sourceDistributionId) {
        String lastDownloadedReleaseHash = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
        if (!TextUtils.isEmpty(lastDownloadedReleaseHash)) {
            if (isCurrentReleaseWasUpdated(lastDownloadedReleaseHash)) {
                AppCenterLog.debug(LOG_TAG, "Successfully reported app update for downloaded release hash (" + lastDownloadedReleaseHash + "), removing from store..");
                SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
                SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID);
            } else {
                AppCenterLog.debug(LOG_TAG, "Stored release hash doesn't match current installation, probably downloaded but not installed yet, keep in store");
            }
        }

        /* Check if state did not change. */
        if (mCheckReleaseCallId == releaseCallId) {

            /* Reset state. */
            mCheckReleaseApiCall = null;

            /* If we didn't know what distribution group we were originally tied to (public track). */
            if (sourceDistributionId == null) {
                processDistributionGroupId(releaseDetails.getDistributionGroupId());
            }

            /* Check minimum Android API level. */
            if (Build.VERSION.SDK_INT >= releaseDetails.getMinApiLevel()) {

                /* Check version code is equals or higher and hash is different. */
                AppCenterLog.debug(LOG_TAG, "Check if latest release is more recent.");
                boolean moreRecent = isMoreRecent(releaseDetails);
                if (!moreRecent) {
                    if (mListener != null && mForegroundActivity != null) {
                        AppCenterLog.debug(LOG_TAG, "Calling listener.onNoReleaseAvailable.");
                        mListener.onNoReleaseAvailable(mForegroundActivity);
                    }
                } else if (canUpdateNow(releaseDetails)) {

                    /* Load last known release to see if we need to prepare a cleanup. */
                    if (mReleaseDetails == null) {
                        updateReleaseDetails(DistributeUtils.loadCachedReleaseDetails());
                    }

                    /* Update cache. */
                    SharedPreferencesManager.putString(PREFERENCE_KEY_RELEASE_DETAILS, rawReleaseDetails);

                    /* If previous release is mandatory and still processing, don't do anything right now. */
                    if (mReleaseDetails != null && mReleaseDetails.isMandatoryUpdate()) {
                        if (mReleaseDetails.getId() != releaseDetails.getId()) {
                            AppCenterLog.debug(LOG_TAG, "Latest release is more recent than the previous mandatory.");
                            SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_AVAILABLE);
                        } else {
                            AppCenterLog.debug(LOG_TAG, "The latest release is mandatory and already being processed.");
                        }
                        return;
                    }

                    /* Prepare download and cleanup older files if needed. */
                    updateReleaseDetails(releaseDetails);

                    /* Show update dialog. */
                    AppCenterLog.debug(LOG_TAG, "Latest release is more recent.");
                    SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_AVAILABLE);
                    if (mForegroundActivity != null) {
                        showUpdateDialog();
                    }
                    return;
                }
            } else {
                AppCenterLog.info(LOG_TAG, "This device is not compatible with the latest release.");
            }

            /* If update dialog was not shown or scheduled, complete workflow. */
            completeWorkflow();
        }
    }

    private synchronized void updateReleaseDetails(ReleaseDetails releaseDetails) {
        if (mReleaseDownloader != null) {

            /* Cancel previous release downloading. */
            if (releaseDetails == null || releaseDetails.getId() != mReleaseDownloader.getReleaseDetails().getId()) {
                mReleaseDownloader.cancel();
            }
            mReleaseDownloader = null;
        } else if (releaseDetails == null) {

            /* When we disable the SDK or cancel every state, we need to clean download cache. */
            ReleaseDownloaderFactory.create(mContext, null, null).cancel();
        }
        if (mReleaseDownloaderListener != null) {
            mReleaseDownloaderListener.hideProgressDialog();
            mReleaseDownloaderListener = null;
        }
        if (mReleaseInstallerListener != null) {
            mReleaseInstallerListener.hideInstallProgressDialog();
            mReleaseInstallerListener = null;
        }
        mReleaseDetails = releaseDetails;
        if (mReleaseDetails != null) {

            /* Create release downloader here to be able correctly cancel downloading from previous runs. */
            mReleaseDownloaderListener = new ReleaseDownloadListener(mContext, mReleaseDetails);
            mReleaseInstallerListener = new ReleaseInstallerListener(mContext);
            mReleaseDownloader = ReleaseDownloaderFactory.create(mContext, mReleaseDetails, mReleaseDownloaderListener);
        }
    }

    /**
     * Get reporting parameters for updated release.
     *
     * @param isPublic            are the parameters for public group or not.
     *                            For public group we report install_id, distribution_group_id and release_id.
     *                            For private group we report distribution_group_id and release_id.
     * @param distributionGroupId distribution group id.
     */
    @NonNull
    private String getReportingParametersForUpdatedRelease(boolean isPublic, String distributionGroupId) {
        String reportingParameters = "";
        AppCenterLog.debug(LOG_TAG, "Check if we need to report release installation..");
        String lastDownloadedReleaseHash = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
        if (!TextUtils.isEmpty(lastDownloadedReleaseHash)) {
            if (isCurrentReleaseWasUpdated(lastDownloadedReleaseHash)) {
                AppCenterLog.debug(LOG_TAG, "Current release was updated but not reported yet, reporting..");
                if (isPublic) {
                    reportingParameters += "&" + PARAMETER_INSTALL_ID + "=" + IdHelper.getInstallId();
                }
                reportingParameters += "&" + PARAMETER_DISTRIBUTION_GROUP_ID + "=" + distributionGroupId;
                int lastDownloadedReleaseId = SharedPreferencesManager.getInt(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID);
                reportingParameters += "&" + PARAMETER_RELEASE_ID + "=" + lastDownloadedReleaseId;
            } else {
                AppCenterLog.debug(LOG_TAG, "New release was downloaded but not installed yet, skip reporting.");
            }
        } else {
            AppCenterLog.debug(LOG_TAG, "Current release was already reported, skip reporting.");
        }
        return reportingParameters;
    }

    /**
     * Check if an updated release has different group ID and update current group ID if needed.
     * Group ID may change if one user is added to different distribution groups and a new release
     * was distributed to another group.
     */
    private void changeDistributionGroupIdAfterAppUpdateIfNeeded() {
        String lastDownloadedReleaseHash = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
        String lastDownloadedDistributionGroupId = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID);
        if (!isCurrentReleaseWasUpdated(lastDownloadedReleaseHash) || TextUtils.isEmpty(lastDownloadedDistributionGroupId)) {
            return;
        }
        String currentDistributionGroupId = SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID);
        if (lastDownloadedDistributionGroupId.equals(currentDistributionGroupId)) {
            return;
        }

        /* Set group ID from downloaded release details. */
        AppCenterLog.debug(LOG_TAG, "Current group ID doesn't match the group ID of downloaded release, updating current group id=" + lastDownloadedDistributionGroupId);
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, lastDownloadedDistributionGroupId);

        /* Remove saved downloaded group ID. */
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID);
    }

    /**
     * Check if latest downloaded release was installed (app was updated).
     *
     * @param lastDownloadedReleaseHash hash of the last downloaded release.
     * @return true if current release was updated.
     */
    private boolean isCurrentReleaseWasUpdated(String lastDownloadedReleaseHash) {
        if (mPackageInfo == null || TextUtils.isEmpty(lastDownloadedReleaseHash)) {
            return false;
        }
        String currentInstalledReleaseHash = computeReleaseHash(mPackageInfo);
        return currentInstalledReleaseHash.equals(lastDownloadedReleaseHash);
    }

    /**
     * Check if the fetched release information should be installed.
     *
     * @param releaseDetails latest release on server.
     * @return true if latest release on server should be used.
     */
    private boolean isMoreRecent(ReleaseDetails releaseDetails) {
        boolean moreRecent;
        int versionCode = DeviceInfoHelper.getVersionCode(mPackageInfo);
        if (releaseDetails.getVersion() == versionCode) {
            moreRecent = !releaseDetails.getReleaseHash().equals(DistributeUtils.computeReleaseHash(mPackageInfo));
        } else {
            moreRecent = releaseDetails.getVersion() > versionCode;
        }
        AppCenterLog.debug(LOG_TAG, "Latest release more recent=" + moreRecent);
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
            AppCenterLog.debug(LOG_TAG, "Release is mandatory, ignoring any postpone action.");
            return true;
        }
        long now = System.currentTimeMillis();
        long postponedTime = SharedPreferencesManager.getLong(PREFERENCE_KEY_POSTPONE_TIME, 0);
        if (now < postponedTime) {
            AppCenterLog.debug(LOG_TAG, "User clock has been changed in past, cleaning postpone state and showing dialog");
            SharedPreferencesManager.remove(PREFERENCE_KEY_POSTPONE_TIME);
            return true;
        }
        long postponedUntil = postponedTime + POSTPONE_TIME_THRESHOLD;
        if (now < postponedUntil) {
            AppCenterLog.debug(LOG_TAG, "Optional updates are postponed until " + new Date(postponedUntil));
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
                    AppCenterLog.debug(LOG_TAG, "Previous dialog is still being shown in the same activity.");
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
            AppCenterLog.debug(LOG_TAG, "Calling listener.onReleaseAvailable.");
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
            AppCenterLog.debug(LOG_TAG, "Show default update dialog.");
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
            dialogBuilder.setTitle(R.string.appcenter_distribute_update_dialog_title);
            final ReleaseDetails releaseDetails = mReleaseDetails;
            String message;
            if (releaseDetails.isMandatoryUpdate()) {
                message = mContext.getString(R.string.appcenter_distribute_update_dialog_message_mandatory);
            } else {
                message = mContext.getString(R.string.appcenter_distribute_update_dialog_message_optional);
            }
            message = formatAppNameAndVersion(message);
            dialogBuilder.setMessage(message);
            dialogBuilder.setPositiveButton(R.string.appcenter_distribute_update_dialog_download, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    enqueueDownloadOrShowUnknownSourcesDialog(releaseDetails);
                }
            });
            dialogBuilder.setCancelable(false);
            if (!releaseDetails.isMandatoryUpdate()) {
                dialogBuilder.setNegativeButton(R.string.appcenter_distribute_update_dialog_postpone, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        postponeRelease(releaseDetails);
                    }
                });
            }
            if (!TextUtils.isEmpty(releaseDetails.getReleaseNotes()) && releaseDetails.getReleaseNotesUrl() != null) {
                dialogBuilder.setNeutralButton(R.string.appcenter_distribute_update_dialog_view_release_notes, new DialogInterface.OnClickListener() {

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
            AppCenterLog.error(LOG_TAG, "Failed to navigate to release notes.", e);
        }
    }

    /**
     * Store failed package info hash in preferences.
     */
    private synchronized void storeUpdateSetupFailedPackageHash(DialogInterface dialog) {
        if (mUpdateSetupFailedDialog == dialog) {
            SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY, DistributeUtils.computeReleaseHash(mPackageInfo));
        } else {
            showDisabledToast();
        }
    }

    /**
     * Redirect user to install url page in browser and clear current failed package hash from preferences.
     */
    private synchronized void handleUpdateFailedDialogReinstallAction(DialogInterface dialog) {
        if (mUpdateSetupFailedDialog == dialog) {

            /* Add a flag to the install url to indicate that the update setup failed, to show a help page. */
            String url = mInstallUrl;
            try {
                url = BrowserUtils.appendUri(url, PARAMETER_UPDATE_SETUP_FAILED + "=" + "true");
            } catch (URISyntaxException e) {
                AppCenterLog.error(LOG_TAG, "Could not append query parameter to url.", e);
            }
            BrowserUtils.openBrowser(url, mForegroundActivity);

            /* Clear the update setup failure info from storage, to re-attempt setup on reinstall. */
            SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
            SharedPreferencesManager.remove(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY);
        } else {
            showDisabledToast();
        }
    }


    /**
     * Show system alerts windows setting dialog.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private synchronized void showSystemAlertsWindowsSettingsDialog() {

        /* Do not attempt to show dialog if application is in the background. */
        if (mForegroundActivity == null) {
            AppCenterLog.warn(LOG_TAG, "The application is in background mode, the system alerts windows won't be displayed.");
            return;
        }

        /* Check if we need to replace dialog. */
        if (!shouldRefreshDialog(mAlertSystemWindowsDialog)) {
            return;
        }
        AppCenterLog.debug(LOG_TAG, "Show new system alerts windows dialog.");

        /* Build confirmation dialog on enabled system alerts windows permission. */
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
        dialogBuilder.setMessage(R.string.appcenter_distribute_alert_system_dialog_message);
        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAlertSystemWindowsDialog = null;
                AppCenterLog.debug(LOG_TAG, "Permission request on alert system windows denied. Continue installing...");

                /* It is optional and installing can be continued if customer reject permission request. */
                installingUpdate();
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mAlertSystemWindowsDialog = null;
                AppCenterLog.debug(LOG_TAG, "Permission request on alert system windows denied. Continue installing...");

                /* It is optional and installing can be continued if customer reject permission request. */
                installingUpdate();
            }
        });
        dialogBuilder.setPositiveButton(R.string.appcenter_distribute_unknown_sources_dialog_settings, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                /* Open system alerts windows settings activity. */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + mForegroundActivity.getPackageName()));
                mForegroundActivity.startActivity(intent);
            }
        });
        mAlertSystemWindowsDialog = dialogBuilder.create();
        showAndRememberDialogActivity(mAlertSystemWindowsDialog);
    }

    /**
     * Show unknown sources dialog. This can be called multiple times if clicking on HOME and app resumed
     * (it could be resumed in another activity covering the previous one).
     */
    @UiThread
    private synchronized void showUnknownSourcesDialog() {

        /* Do not attempt to show dialog if application is in the background. */
        if (mForegroundActivity == null) {
            AppCenterLog.warn(LOG_TAG, "The application is in background mode, the unknown sources dialog won't be displayed.");
            return;
        }

        /* Check if we need to replace dialog. */
        if (!shouldRefreshDialog(mUnknownSourcesDialog)) {
            return;
        }
        AppCenterLog.debug(LOG_TAG, "Show new unknown sources dialog.");

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
        dialogBuilder.setMessage(R.string.appcenter_distribute_unknown_sources_dialog_message);
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
        dialogBuilder.setPositiveButton(R.string.appcenter_distribute_unknown_sources_dialog_settings, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                goToUnknownAppsSettings(releaseDetails);
            }
        });
        mUnknownSourcesDialog = dialogBuilder.create();
        showAndRememberDialogActivity(mUnknownSourcesDialog);
    }

    /**
     * Show update setup failed dialog.
     */
    @UiThread
    private synchronized void showUpdateSetupFailedDialog() {

        /* Check if we need to replace the dialog. */
        if (!shouldRefreshDialog(mUpdateSetupFailedDialog)) {
            return;
        }
        AppCenterLog.debug(LOG_TAG, "Show update setup failed dialog.");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setTitle(R.string.appcenter_distribute_update_failed_dialog_title);
        dialogBuilder.setMessage(R.string.appcenter_distribute_update_failed_dialog_message);
        dialogBuilder.setPositiveButton(R.string.appcenter_distribute_update_failed_dialog_ignore, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                storeUpdateSetupFailedPackageHash(dialog);
            }
        });
        dialogBuilder.setNegativeButton(R.string.appcenter_distribute_update_failed_dialog_reinstall, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                handleUpdateFailedDialogReinstallAction(dialog);
            }
        });
        mUpdateSetupFailedDialog = dialogBuilder.create();
        showAndRememberDialogActivity(mUpdateSetupFailedDialog);

        /* Don't show this dialog again. */
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    /**
     * Navigate to security settings or application settings on Android O.
     *
     * @param releaseDetails release details to check for state change.
     */
    private synchronized void goToUnknownAppsSettings(ReleaseDetails releaseDetails) {
        Intent intent;

        /* Do not attempt to show dialog if application is in the background. */
        if (mForegroundActivity == null) {
            AppCenterLog.warn(LOG_TAG, "The application is in background mode, the settings screen could not be opened.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + mForegroundActivity.getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        }
        try {

            /*
             * We can't use startActivityForResult as we don't subclass activities.
             * And a no U.I. activity of our own must finish in onCreate,
             * so it cannot receive a result.
             */
            mForegroundActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {

            /* On some devices, it's not possible, user will do it by himself. */
            AppCenterLog.warn(LOG_TAG, "No way to navigate to secure settings on this device automatically");

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
            AppCenterLog.debug(LOG_TAG, "Postpone updates for a day.");
            SharedPreferencesManager.putLong(PREFERENCE_KEY_POSTPONE_TIME, System.currentTimeMillis());
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
                AppCenterLog.debug(LOG_TAG, "Schedule download...");
                resumeDownload();

                /* Refresh mandatory dialog progress or do nothing otherwise. */
                showDownloadProgress();

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
        Toast.makeText(mContext, R.string.appcenter_distribute_dialog_actioned_on_disabled_toast, Toast.LENGTH_SHORT).show();
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

    @UiThread
    synchronized void notifyInstallProgress(boolean isInProgress) {
        mInstallInProgress = isInProgress;
        if (isInProgress) {

            /* Do not attempt to show dialog if application is in the background. */
            if (mForegroundActivity == null) {
                AppCenterLog.warn(LOG_TAG, "Could not display install progress dialog in the background.");
                return;
            }
            if (mReleaseInstallerListener == null) {
                return;
            }

            /* Close to avoid dialog duplicates. */
            mReleaseInstallerListener.hideInstallProgressDialog();

            /* Create and show a new dialog. */
            Dialog progressDialog = mReleaseInstallerListener.showInstallProgressDialog(mForegroundActivity);
            showAndRememberDialogActivity(progressDialog);
        } else {
            if (mReleaseInstallerListener != null) {
                mReleaseInstallerListener.hideInstallProgressDialog();
                mReleaseInstallerListener = null;
            }
        }
    }

    /**
     * Start to install a new update.
     */
    synchronized private void installingUpdate() {
        if (mReleaseInstallerListener == null) {
            AppCenterLog.debug(LOG_TAG, "Installing couldn't start due to the release installer wasn't initialized.");
            return;
        }
        mReleaseInstallerListener.startInstall();
    }

    /**
     * Ask permission on start application after update or start to install a new update.
     */
    synchronized void showSystemSettingsDialogOrStartInstalling(long downloadId) {
        if (mReleaseInstallerListener == null) {
            AppCenterLog.debug(LOG_TAG, "Couldn't set 'downloadId' value due to the release installer wasn't initialized.");
            return;
        }
        mReleaseInstallerListener.setDownloadId(downloadId);

        /* Check permission on start application after update. */
        if (InstallerUtils.isSystemAlertWindowsEnabled(mContext)) {
            installingUpdate();
        } else {
            showSystemAlertsWindowsSettingsDialog();
        }
    }

    /**
     * Post notification about a completed download if we are in background when download completes.
     * If this method is called on app process restart or if application is in foreground
     * when download completes, it will not notify and return that the install U.I. should be shown now.
     *
     * @param releaseDetails release details to check state.
     * @return false if install U.I should be shown now, true if a notification was posted or if the task was canceled.
     */
    @UiThread
    synchronized boolean notifyDownload(ReleaseDetails releaseDetails) {

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
        AppCenterLog.debug(LOG_TAG, "Post a notification as the download finished in background.");
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            /* Create or update notification channel (mandatory on Android 8 target). */
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    mContext.getString(R.string.appcenter_distribute_notification_category),
                    NotificationManager.IMPORTANCE_DEFAULT);

            //noinspection ConstantConditions
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(mContext, NOTIFICATION_CHANNEL_ID);
        } else {
            builder = getOldNotificationBuilder();
        }
        builder.setTicker(mContext.getString(R.string.appcenter_distribute_install_ready_title))
                .setContentTitle(mContext.getString(R.string.appcenter_distribute_install_ready_title))
                .setContentText(getInstallReadyMessage())
                .setSmallIcon(mContext.getApplicationInfo().icon);
        builder.setStyle(new Notification.BigTextStyle().bigText(getInstallReadyMessage()));
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        //noinspection ConstantConditions
        notificationManager.notify(DistributeUtils.getNotificationId(), notification);
        SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_NOTIFIED);

        /* Reset check download flag to show install U.I. on resume if notification ignored. */
        mCheckedDownload = false;
        return true;
    }

    @NonNull
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private Notification.Builder getOldNotificationBuilder() {
        return new Notification.Builder(mContext);
    }

    /**
     * Show download progress (used only for mandatory updates).
     */
    private synchronized void showDownloadProgress() {

        /* Do not attempt to show dialog if application is in the background. */
        if (mForegroundActivity == null) {
            AppCenterLog.warn(LOG_TAG, "Could not display progress dialog in the background.");
            return;
        }
        if (mReleaseDownloaderListener == null) {
            return;
        }
        Dialog progressDialog = mReleaseDownloaderListener.showDownloadProgress(mForegroundActivity);

        /*
         * It can be null in cases when it wasn't been created
         * (for example progress dialog is required only for mandatory updates).
         */
        if (progressDialog != null) {
            showAndRememberDialogActivity(progressDialog);
        }
    }

    /**
     * Show modal dialog with install button if mandatory update ready and user cancelled install.
     */
    private synchronized void showMandatoryDownloadReadyDialog() {
        if (!shouldRefreshDialog(mCompletedDownloadDialog)) {
            return;
        }
        final ReleaseDetails releaseDetails = mReleaseDetails;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mForegroundActivity);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setTitle(R.string.appcenter_distribute_install_ready_title);
        dialogBuilder.setMessage(getInstallReadyMessage());
        dialogBuilder.setPositiveButton(R.string.appcenter_distribute_install, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                installMandatoryUpdate(releaseDetails);
            }
        });
        mCompletedDownloadDialog = dialogBuilder.create();
        showAndRememberDialogActivity(mCompletedDownloadDialog);
    }

    /**
     * Get text for app is ready to be installed.
     */
    private String getInstallReadyMessage() {
        return formatAppNameAndVersion(mContext.getString(R.string.appcenter_distribute_install_ready_message));
    }

    /**
     * Inject app name version and version code in a format string.
     */
    private String formatAppNameAndVersion(String format) {
        String appName = AppNameHelper.getAppName(mContext);
        return String.format(format, appName, mReleaseDetails.getShortVersion(), mReleaseDetails.getVersion());
    }

    /**
     * Install mandatory update after clicking on the install dialog button.
     *
     * @param releaseDetails release details.
     */
    private synchronized void installMandatoryUpdate(ReleaseDetails releaseDetails) {
        if (releaseDetails == mReleaseDetails) {
            resumeDownload();
        } else {
            showDisabledToast();
        }
    }

    /**
     * Resume downloading installer for current {@link ReleaseDetails}.
     */
    synchronized void resumeDownload() {
        if (mReleaseDownloader != null) {
            mReleaseDownloader.resume();
            mCheckedDownload = true;
        }
    }

    /**
     * Update download state to downloading if state did not change.
     *
     * @param releaseDetails to check state change.
     * @param enqueueTime    timestamp in milliseconds just before enqueuing download.
     */
    @UiThread
    synchronized void setDownloading(@NonNull ReleaseDetails releaseDetails, long enqueueTime) {
        if (releaseDetails != mReleaseDetails) {
            return;
        }
        SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_ENQUEUED);
        SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_TIME, enqueueTime);
    }

    /**
     * Update download state to installing if state did not change.
     *
     * @param releaseDetails to check state change.
     */
    @UiThread
    synchronized void setInstalling(@NonNull ReleaseDetails releaseDetails) {
        if (releaseDetails != mReleaseDetails) {
            return;
        }
        if (releaseDetails.isMandatoryUpdate()) {
            cancelNotification();
            SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_INSTALLING);
        } else {
            completeWorkflow(releaseDetails);
        }
        String groupId = releaseDetails.getDistributionGroupId();
        String releaseHash = releaseDetails.getReleaseHash();
        int releaseId = releaseDetails.getId();
        AppCenterLog.debug(LOG_TAG, "Stored release details: group id=" + groupId + " release hash=" + releaseHash + " release id=" + releaseId);
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID, groupId);
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH, releaseHash);
        SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID, releaseId);
    }

    /**
     * Send distribution start session log after enabling in-app updates (first app launch after installation).
     */
    private synchronized void enqueueDistributionStartSessionLog() {

        /*
         * Session starts before in-app updates setup (using browser) so the first start session log
         * is sent without distributionGroupId value.
         *
         * Send the distribution start session log if start session log without distributionGroupId
         * value was sent before
         */
        SessionContext.SessionInfo lastSession = SessionContext.getInstance().getSessionAt(System.currentTimeMillis());
        if (lastSession == null || lastSession.getSessionId() == null) {
            AppCenterLog.debug(LOG_TAG, "No sessions were logged before, ignore sending of the distribution start session log.");
            return;
        }
        post(new Runnable() {

            @Override
            public void run() {
                DistributionStartSessionLog log = new DistributionStartSessionLog();
                mChannel.enqueue(log, DISTRIBUTE_GROUP, Flags.DEFAULTS);
            }
        });
    }
}
