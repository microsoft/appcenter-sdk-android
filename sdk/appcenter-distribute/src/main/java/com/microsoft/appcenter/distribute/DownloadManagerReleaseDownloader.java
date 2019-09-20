package com.microsoft.appcenter.distribute;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.microsoft.appcenter.distribute.CheckDownloadTask;
import com.microsoft.appcenter.distribute.DistributeConstants;
import com.microsoft.appcenter.distribute.DistributeUtils;
import com.microsoft.appcenter.distribute.DownloadTask;
import com.microsoft.appcenter.distribute.InstallerUtils;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.RemoveDownloadTask;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_AVAILABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_INSTALLING;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static com.microsoft.appcenter.distribute.DistributeUtils.getStoredDownloadState;

public class DownloadManagerReleaseDownloader implements ReleaseDownloader {

    private Context mContext;
    private Listener mListener;
    private long downloadId;

    /**
     * Current task to check download state and act on it.
     */
    private CheckDownloadTask mCheckDownloadTask;

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private DownloadTask mDownloadTask;

    /**
     * Remember if we checked download since our own process restarted.
     */
    private boolean mCheckedDownload;

    /**
     * Package info.
     */
    private PackageInfo mPackageInfo;


    DownloadManagerReleaseDownloader(Context context) {
        mContext = context;
        try {
            mPackageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            AppCenterLog.error(LOG_TAG, "Could not get self package info.", e);
        }
    }

    @Override
    public void download(ReleaseDetails releaseDetails) {

        /* Load cached release details if process restarted and we have such a cache. */
        int downloadState = getStoredDownloadState();
        if (releaseDetails == null && downloadState != DOWNLOAD_STATE_COMPLETED) {
            releaseDetails = DistributeUtils.loadCachedReleaseDetails();

            /* If cached release is optional and we have network, we should not reuse it. */
            if (releaseDetails != null && !releaseDetails.isMandatoryUpdate() &&
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

                /* If app restarted, check if download completed to bring install U.I. */
                // todo call CheckDownloadTask

                mCheckedDownload = true;

                /* If downloading mandatory update proceed to restore progress dialog in the meantime. */
                if (releaseDetails == null || !releaseDetails.isMandatoryUpdate() || downloadState != DOWNLOAD_STATE_ENQUEUED) {
                    return;
                }
            }
        }

        /*
         * If we got a release information but application backgrounded then resumed,
         * check what dialog to restore.
         */
        if (releaseDetails != null) {

            /* If we go back to application without installing the mandatory update. */
            if (downloadState == DOWNLOAD_STATE_INSTALLING) {

                /* Show a new modal dialog with only install button. */
                if (mListener != null){
                    mListener.onComplete("");
                }
            }

            /* If we are still downloading. */
            else if (downloadState == DOWNLOAD_STATE_ENQUEUED) {

                /* Refresh mandatory dialog progress or do nothing otherwise. */
                if (releaseDetails.isMandatoryUpdate()) {

                    // todo call CheckDownloadTask and show progress dialog
                }
            }

//            /* If we were showing unknown sources dialog, restore it. */
//            else if (mUnknownSourcesDialog != null) {
//
//                /*
//                 * Resume click download step if last time we were showing unknown source dialog.
//                 * Note that we could be executed here after going to enable settings and being back in app.
//                 * We can start download if the setting is now enabled,
//                 * otherwise restore dialog if activity rotated or was covered.
//                 */
//                enqueueDownloadOrShowUnknownSourcesDialog(releaseDetails);
//            }

            /*
             * Or restore update dialog if that's the last thing we did before being paused.
             * Also checking we are not about to download (DownloadTask might still be running and thus not enqueued yet).
             */
            else if (mDownloadTask == null) {

                // todo show update dialog
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
    }

    private void startDownloadTask(ReleaseDetails releaseDetails) {
        mDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadTask(mContext, releaseDetails));
    }

    private void startCheckDownloadTask(ReleaseDetails releaseDetails) {
        boolean checkProgress = true;

        /* Querying download manager and even the start intent are detected by strict mode so we do that in background. */
        mCheckDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new CheckDownloadTask(mContext, downloadId, checkProgress, releaseDetails));
    }

    @Override
    public void delete() {
        AsyncTaskUtils.execute(LOG_TAG, new RemoveDownloadTask(mContext, downloadId));
    }

    /**
     * Cancel everything.
     */
    private synchronized void cancelPreviousTasks() {
        // todo cancel previous tasks and dialogs
        // todo remove download file
        // todo clear preferences
        mCheckedDownload = false;
    }


    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }
}
