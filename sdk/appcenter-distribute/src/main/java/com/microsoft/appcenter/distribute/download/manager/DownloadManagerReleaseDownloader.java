/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.R;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.AbstractReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.HANDLER_TOKEN_CHECK_PROGRESS;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_PROGRESS_TIME_THRESHOLD;

public class DownloadManagerReleaseDownloader extends AbstractReleaseDownloader {

    public DownloadManagerReleaseDownloader(@NonNull Context context, @NonNull ReleaseDetails releaseDetails, @NonNull Listener listener) {
        super(context, releaseDetails, listener);
    }

    private long mDownloadId = INVALID_DOWNLOAD_IDENTIFIER;

    /**
     * Current task to check download state and act on it.
     */
    private DownloadManagerUpdateTask mUpdateTask;

    /**
     * Current task inspecting the latest release details that we fetched from server.
     */
    private DownloadManagerRequestTask mRequestTask;

    DownloadManager getDownloadManager() {
        return (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
    }

    @WorkerThread
    synchronized long getDownloadId() {
        if (mDownloadId == INVALID_DOWNLOAD_IDENTIFIER) {
            mDownloadId = SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
        }
        return mDownloadId;
    }

    @WorkerThread
    private synchronized void setDownloadId(long downloadId) {
        mDownloadId = downloadId;
        if (mDownloadId != INVALID_DOWNLOAD_IDENTIFIER) {
            SharedPreferencesManager.putLong(PREFERENCE_KEY_DOWNLOAD_ID, downloadId);
        } else {
            SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        }
    }

    @Override
    public synchronized boolean isDownloading() {
        return mDownloadId != INVALID_DOWNLOAD_IDENTIFIER;
    }

    @AnyThread
    @Override
    public synchronized void resume() {

        /*
         * Just update the current downloading status.
         * All checks will be performed in the background thread.
         */
        update();
    }

    @Override
    public synchronized void cancel() {
        if (isCancelled()) {
            return;
        }
        super.cancel();
        if (mRequestTask != null) {
            mRequestTask.cancel(true);
            mRequestTask = null;
        }
        if (mUpdateTask != null) {
            mUpdateTask.cancel(true);
            mUpdateTask = null;
        }
        long downloadId = getDownloadId();
        if (downloadId != INVALID_DOWNLOAD_IDENTIFIER) {
            remove(downloadId);
            setDownloadId(INVALID_DOWNLOAD_IDENTIFIER);
        }
    }

    /**
     * Start new download.
     */
    private synchronized void request() {
        if (isCancelled()) {
            return;
        }
        if (mRequestTask != null) {
            AppCenterLog.debug(LOG_TAG, "Downloading is already in progress.");
            return;
        }
        mRequestTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadManagerRequestTask(this, mContext.getString(R.string.appcenter_distribute_downloading_version)));
    }

    /**
     * Update the state on current download.
     */
    private synchronized void update() {
        if (isCancelled()) {
            return;
        }
        mUpdateTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadManagerUpdateTask(this));
    }

    private void remove(long downloadId) {
        AppCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
        AsyncTaskUtils.execute(LOG_TAG, new DownloadManagerRemoveTask(mContext, downloadId));
    }

    @WorkerThread
    synchronized void onStart() {
        request();
    }

    @WorkerThread
    synchronized void onDownloadStarted(long downloadId, long enqueueTime) {
        if (isCancelled()) {
            return;
        }

        /* Store new download identifier. */
        setDownloadId(downloadId);
        mListener.onStart(enqueueTime);

        /* Start monitoring progress for mandatory update. */
        if (mReleaseDetails.isMandatoryUpdate()) {
            update();
        }
    }

    @WorkerThread
    synchronized void onDownloadProgress(Cursor cursor) {
        if (isCancelled()) {
            return;
        }
        long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        long currentSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        if (mListener.onProgress(currentSize, totalSize)) {

            /* Schedule the next check if more updates are needed. */
            HandlerUtils.getMainHandler().postAtTime(new Runnable() {

                @Override
                public void run() {
                    update();
                }
            }, HANDLER_TOKEN_CHECK_PROGRESS, SystemClock.uptimeMillis() + UPDATE_PROGRESS_TIME_THRESHOLD);
        }
    }

    @WorkerThread
    synchronized void onDownloadComplete(Cursor cursor) {
        if (isCancelled()) {
            return;
        }
        AppCenterLog.debug(LOG_TAG, "Download was successful for id=" + mDownloadId);
        Uri localUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)));
        boolean installerFound = false;
        if (!mListener.onComplete(localUri)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                installerFound = mListener.onComplete(getFileUriOnOldDevices(cursor));
            }
        } else {
            installerFound = true;
        }
        if (!installerFound) {
            mListener.onError("Installer not found");
        }
    }

    @WorkerThread
    synchronized void onDownloadError(RuntimeException e) {
        if (isCancelled()) {
            return;
        }
        AppCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId, e);
        mListener.onError(e.getMessage());
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static Uri getFileUriOnOldDevices(Cursor cursor) throws IllegalArgumentException {
        return Uri.parse("file://" + cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)));
    }
}
