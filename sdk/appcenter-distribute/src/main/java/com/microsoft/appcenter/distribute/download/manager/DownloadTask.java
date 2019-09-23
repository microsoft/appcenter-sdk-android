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
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloadListener;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader.PREFERENCE_PREFIX;
import static com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader.getStoredDownloadId;

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
     * We are waiting to hear back from download manager, we may poll status on process restart.
     */
    static final int DOWNLOAD_STATE_ENQUEUED = 2;

    /**
     * Preference key to store the current/last download identifier (we keep download until a next
     * one is scheduled as the file can be opened from device downloads U.I.).
     */
    static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Preference key to store the SDK state related to {@link #PREFERENCE_KEY_DOWNLOAD_ID} when not null.
     */
    static final String PREFERENCE_KEY_DOWNLOAD_STATE = PREFERENCE_PREFIX + "download_state";

    /**
     * Preference key to store download start time. Used to avoid showing install U.I. of a completed
     * download if we already updated (the download workflow can work across process restarts).
     * <p>
     * We can't use {@link DownloadManager#COLUMN_LAST_MODIFIED_TIMESTAMP} as we could have a corner case
     * where we install upgrade from email or another mean while waiting download triggered by SDK.
     * So the time we store as a reference needs to be before download time.
     */
    static final String PREFERENCE_KEY_DOWNLOAD_TIME = PREFERENCE_PREFIX + "download_time";

    /**
     * Release details to check.
     */
    private final ReleaseDetails mReleaseDetails;

    /**
     * Listener for download states.
     */
    private ReleaseDownloader.Listener mListener;

    /**
     * Init.
     *
     * @param context        context.
     * @param releaseDetails release details associated to this check.
     */
    DownloadTask(Context context, ReleaseDetails releaseDetails, ReleaseDownloader.Listener listener) {
        mContext = context;
        mReleaseDetails = releaseDetails;
        mListener = listener;
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
            DownloadManagerReleaseDownloader.removePreviousDownloadId(downloadManager);

            /* Store new download identifier. */
            SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadRequestId);
            SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_ENQUEUED);
            SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_TIME, enqueueTime);

            /* Start monitoring progress for mandatory update. */
            if (mReleaseDetails.isMandatoryUpdate()) {
                // todo handle listener check progress

            }
        } else {

            /* State changed quickly, cancel download. */
            AppCenterLog.debug(LOG_TAG, "State changed while downloading, cancel id=" + downloadRequestId);
            downloadManager.remove(downloadRequestId);
        }
        return null;
    }
}
