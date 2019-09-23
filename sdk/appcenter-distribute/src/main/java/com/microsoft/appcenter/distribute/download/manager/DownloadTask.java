/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.DistributeUtils;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;

/**
 * The download manager API triggers strict mode exception in U.I. thread.
 */
class DownloadTask extends AsyncTask<Void, Void, Void> {

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    /**
     * Release details to check.
     */
    private final ReleaseDetails mReleaseDetails;

    /**
     * Init.
     *
     * @param context        context.
     * @param releaseDetails release details associated to this check.
     */
    DownloadTask(Context context, ReleaseDetails releaseDetails) {
        mContext = context;
        mReleaseDetails = releaseDetails;
    }

    @Override
    protected Void doInBackground(Void[] params) {

        /* Download file. */
        Uri downloadUrl = mReleaseDetails.getDownloadUrl();
        AppCenterLog.debug(LOG_TAG, "Start downloading new release, url=" + downloadUrl);
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(downloadUrl);

        /* Hide mandatory download to prevent canceling via notification cancel or download U.I. delete. */
        if (mReleaseDetails.isMandatoryUpdate()) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setVisibleInDownloadsUi(false);
        }
        long enqueueTime = System.currentTimeMillis();

        @SuppressWarnings("ConstantConditions")
        long downloadRequestId = downloadManager.enqueue(request);

        /* Check for if state changed and task not canceled in time. */
        if (mReleaseDetails != null) {

            /* Delete previous download. */
            long previousDownloadId = DistributeUtils.getStoredDownloadId();
            if (previousDownloadId >= 0) {
                AppCenterLog.debug(LOG_TAG, "Delete previous download id=" + previousDownloadId);
                downloadManager.remove(previousDownloadId);
            }

            /* Store new download identifier. */
            SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadRequestId);
            SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_ENQUEUED);
            SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_TIME, enqueueTime);

            /* Start monitoring progress for mandatory update. */
            if (mReleaseDetails.isMandatoryUpdate()) {
                checkDownload(mContext, previousDownloadId, true);
            }
        } else {

            /* State changed quickly, cancel download. */
            AppCenterLog.debug(LOG_TAG, "State changed while downloading, cancel id=" + downloadRequestId);
            downloadManager.remove(downloadRequestId);
        }
        return null;
    }
}
