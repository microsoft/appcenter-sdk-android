package com.microsoft.appcenter.distribute.download.manager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.PluralsRes;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.DistributeUtils;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.CheckDownloadTask;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.distribute.download.RemoveDownloadTask;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;

public class DownloadManagerReleaseDownloader implements ReleaseDownloader {
    private Context mContext;
    private Listener mListener;

    /**
     * Remember if we checked download since our own process restarted.
     */
    private boolean mCheckedDownload;

    /**
     * Current task to check download state and act on it.
     */
    private CheckDownloadTask mCheckDownloadTask;

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private DownloadTask mDownloadTask;

    /**
     * Package info.
     */
    private PackageInfo mPackageInfo;

    /**
     * Distribute service name.
     */
    static final String SERVICE_NAME = "Distribute";

    /**
     * Base key for stored preferences.
     */
    static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store the current/last download identifier (we keep download until a next
     * one is scheduled as the file can be opened from device downloads U.I.).
     */
    static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";


    public DownloadManagerReleaseDownloader(Context context) {
        this.mContext = context;
        try {
            mPackageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            AppCenterLog.error(LOG_TAG, "Could not get self package info.", e);
        }
    }

    @Override
    public void download(ReleaseDetails releaseDetails, Listener listener) {
        // todo handle listener

        long downloadId = getStoredDownloadId();
        if (releaseDetails.isMandatoryUpdate() || mCheckedDownload) {
            checkDownload(mContext, downloadId, true, releaseDetails);
        } else {
            mDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadTask(mContext, releaseDetails));
            mCheckedDownload = true;
        }
    }

    @Override
    public void delete() {
        long downloadId = getStoredDownloadId();
        if (downloadId >= 0) {

            AppCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
            AsyncTaskUtils.execute(LOG_TAG, new RemoveDownloadTask(mContext, downloadId));
        }

        SharedPreferencesManager.remove(PREFERENCE_KEY_RELEASE_DETAILS);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_TIME);
    }


    public void removeListener() {
        mListener = null;
        mCheckDownloadTask.detachListener();
    }

    public void cancel() {
        mDownloadTask.cancel(true);
        mCheckDownloadTask.cancel(true);

    }

    static long getStoredDownloadId() {
        return SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
    }

    /**
     * Check a download state and take action depending on that state.
     *
     * @param context       any application context.
     * @param downloadId    download identifier from DownloadManager.
     * @param checkProgress true to only check progress, false to also process install if done.
     */
    synchronized void checkDownload(@NonNull Context context, long downloadId, boolean checkProgress, ReleaseDetails releaseDetails) {

        /* Querying download manager and even the start intent are detected by strict mode so we do that in background. */
        mCheckDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new CheckDownloadTask(context, downloadId, checkProgress, releaseDetails));
        mCheckDownloadTask.attachListener(mListener);
    }

    /**
     * Used to avoid querying download manager on every activity change.
     *
     * @param releaseDetails release details to check state.
     */
    synchronized void setDownloadState(ReleaseDetails releaseDetails, boolean state) {
        if (releaseDetails == releaseDetails) {
            AppCenterLog.verbose(LOG_TAG, "Download is still in progress...");
            mCheckedDownload = state;
        }
    }

    /**
     * Persist download state.
     *
     * @param downloadManager download manager.
     * @param task            current task to check state change.
     * @param downloadId      download identifier.
     * @param enqueueTime     time just before enqueuing download.
     */
    // TODO Move to DownloadManagerReleaseDownloader
    @WorkerThread
    synchronized void storeDownloadRequestId(DownloadManager downloadManager, ReleaseDownloader task, long downloadId, long enqueueTime) {

        /* Check for if state changed and task not canceled in time. */
        if (mReleaseDownloader == task && mReleaseDetails != null) {

            /* Delete previous download. */
            long previousDownloadId = DistributeUtils.getStoredDownloadId();
            if (previousDownloadId >= 0) {
                AppCenterLog.debug(LOG_TAG, "Delete previous download id=" + previousDownloadId);
                downloadManager.remove(previousDownloadId);
            }

            /* Store new download identifier. */
            SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadId);
            SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_ENQUEUED);
            SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_TIME, enqueueTime);
            mReleaseDownloader.download(mReleaseDetails);
        } else {

            /* State changed quickly, cancel download. */
            AppCenterLog.debug(LOG_TAG, "State changed while downloading, cancel id=" + downloadId);
            downloadManager.remove(downloadId);
        }
    }
}
