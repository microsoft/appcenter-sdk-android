/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.NoSuchElementException;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

/**
 * Inspect a pending or completed download.
 * This uses APIs that would trigger strict mode exception if used in U.I. thread.
 */
public class CheckDownloadTask extends AsyncTask<Void, Void, DownloadProgress> {

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    /**
     * Download identifier to inspect.
     */
    private final long mDownloadId;

    /**
     * Flag to only check progress and not notify or show install U.I. if checking progress while
     * download completed in the meantime.
     */
    private final boolean mCheckProgress;

    /**
     * Release details.
     */
    private ReleaseDetails mReleaseDetails;

    /**
     * Listener for download states.
     */
    private ReleaseDownloader.Listener mListener;

    /**
     * Init.
     *
     * @param context        context.
     * @param downloadId     download identifier.
     * @param checkProgress  check progress only.
     * @param releaseDetails release details.
     */
    public CheckDownloadTask(Context context, long downloadId, boolean checkProgress, ReleaseDetails releaseDetails) {
        mContext = context.getApplicationContext();
        mDownloadId = downloadId;
        mCheckProgress = checkProgress;
        mReleaseDetails = releaseDetails;
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static Uri getFileUriOnOldDevices(Cursor cursor) throws IllegalArgumentException {
        return Uri.parse("file://" + cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)));
    }

    @Override
    protected DownloadProgress doInBackground(Void... params) {

        /*
         * Completion might be triggered in background before AppCenter.start
         * if application was killed after starting download.
         *
         * We still want to generate the notification: if we can find the data in preferences
         * that means they were not deleted, and thus that the sdk was not disabled.
         */
        AppCenterLog.debug(LOG_TAG, "Check download id=" + mDownloadId);
        Distribute distribute = Distribute.getInstance();
        if (mReleaseDetails == null) {
            mReleaseDetails = distribute.startFromBackground(mContext);
        }

        /* Check intent data is what we expected. */
        long expectedDownloadId = DownloadUtils.getStoredDownloadId();
        if (expectedDownloadId == DownloadUtils.INVALID_DOWNLOAD_IDENTIFIER || expectedDownloadId != mDownloadId) {
            AppCenterLog.debug(LOG_TAG, "Ignoring download identifier we didn't expect, id=" + mDownloadId);
            return null;
        }

        /* Query download manager. */
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        try {

            @SuppressWarnings("ConstantConditions")
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
            if (cursor == null) {
                throw new NoSuchElementException();
            }
            try {
                if (!cursor.moveToFirst()) {
                    throw new NoSuchElementException();
                }
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_FAILED) {
                    throw new IllegalStateException();
                }
                if (status != DownloadManager.STATUS_SUCCESSFUL || mCheckProgress) {
                    if (mCheckProgress) {
                        long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        long currentSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        AppCenterLog.verbose(LOG_TAG, "currentSize=" + currentSize + " totalSize=" + totalSize);
                        return new DownloadProgress(currentSize, totalSize);
                    } else {
                        distribute.markDownloadStillInProgress(mReleaseDetails);
                        return null;
                    }
                }

                /* Build install intent. */
                String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                AppCenterLog.debug(LOG_TAG, "Download was successful for id=" + mDownloadId + " uri=" + localUri);
                Intent intent = DownloadUtils.getInstallIntent(Uri.parse(localUri));
                boolean installerFound = false;
                if (intent.resolveActivity(mContext.getPackageManager()) == null) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        intent = DownloadUtils.getInstallIntent(getFileUriOnOldDevices(cursor));
                        installerFound = intent.resolveActivity(mContext.getPackageManager()) != null;
                    }
                } else {
                    installerFound = true;
                }
                if (!installerFound) {
                    AppCenterLog.error(LOG_TAG, "Installer not found");
                    distribute.completeWorkflow(mReleaseDetails);
                    return null;
                }

                /* Check if a should install now. */
                if (!distribute.notifyDownload(mReleaseDetails, intent)) {

                    /*
                     * This start call triggers strict mode in U.I. thread so it
                     * needs to be done here without synchronizing
                     * (not to block methods waiting on synchronized on U.I. thread)
                     * so yes we could launch install and SDK being disabled...
                     *
                     * This corner case cannot be avoided without triggering
                     * strict mode exception.
                     */
                    AppCenterLog.info(LOG_TAG, "Show install UI now intentUri=" + intent.getData());
                    mContext.startActivity(intent);
                    if (mReleaseDetails != null && mReleaseDetails.isMandatoryUpdate()) {
                        distribute.setInstalling(mReleaseDetails);
                    } else {
                        distribute.completeWorkflow(mReleaseDetails);
                    }
                    storeDownloadedReleaseDetails();
                }
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId, e);
            distribute.completeWorkflow(mReleaseDetails);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final DownloadProgress result) {
        if (result != null) {

            /* onPostExecute is not always called on UI thread due to an old Android bug. */
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Distribute.getInstance().updateProgressDialog(mReleaseDetails, result);
                }
            });
        }
    }

    public void attachListener(ReleaseDownloader.Listener listener) {
        mListener = listener;
    }

    public void detachListener() {
        mListener = null;
    }

    /**
     * Store details about downloaded release.
     * After app update and restart, this info is used to report new download and to update group ID (if it's changed).
     */
    private void storeDownloadedReleaseDetails() {
        if (mReleaseDetails == null) {
            AppCenterLog.debug(LOG_TAG, "Downloaded release details are missing or broken, won't store.");
            return;
        }
        String groupId = mReleaseDetails.getDistributionGroupId();
        String releaseHash = mReleaseDetails.getReleaseHash();
        int releaseId = mReleaseDetails.getId();
        AppCenterLog.debug(LOG_TAG, "Store downloaded group id=" + groupId + " release hash=" + releaseHash + " release id=" + releaseId);
        SharedPreferencesManager.putString(DownloadUtils.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID, groupId);
        SharedPreferencesManager.putString(DownloadUtils.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH, releaseHash);
        SharedPreferencesManager.putInt(DownloadUtils.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID, releaseId);
    }
}