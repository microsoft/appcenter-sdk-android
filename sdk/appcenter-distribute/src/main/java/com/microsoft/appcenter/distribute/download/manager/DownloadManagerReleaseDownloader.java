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
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.NoSuchElementException;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS;
import static com.microsoft.appcenter.distribute.DistributeConstants.HANDLER_TOKEN_CHECK_PROGRESS;
import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;

public class DownloadManagerReleaseDownloader implements ReleaseDownloader {

    /**
     * Context.
     */
    private final Context mContext;

    /**
     * Release to download.
     */
    private final ReleaseDetails mReleaseDetails;

    /**
     * Listener of download status.
     */
    private final ReleaseDownloader.Listener mListener;

    public DownloadManagerReleaseDownloader(@NonNull Context context, @NonNull ReleaseDetails releaseDetails, @NonNull ReleaseDownloader.Listener listener) {
        mContext = context;
        mReleaseDetails = releaseDetails;
        mListener = listener;
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


    private DownloadManager getDownloadManager() {
        return (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
    }

    private synchronized long getDownloadId() {
        if (mDownloadId == INVALID_DOWNLOAD_IDENTIFIER) {
            mDownloadId = SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
        }
        return mDownloadId;
    }

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

    @NonNull
    @Override
    public ReleaseDetails getReleaseDetails() {
        return mReleaseDetails;
    }

    @Override
    public void resume() {

        /* If there is active downloads. */
        long downloadId = getDownloadId();
        if (downloadId != INVALID_DOWNLOAD_IDENTIFIER) {
            update();
        } else {
            request();
        }
    }

    @Override
    public synchronized void cancel() {
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
    private void request() {
        mRequestTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadManagerRequestTask(this));
    }

    /**
     * Update the state on current download.
     */
    private void update() {
        mUpdateTask = AsyncTaskUtils.execute(LOG_TAG, new DownloadManagerUpdateTask(this));
    }

    private void remove(long downloadId) {
        AppCenterLog.debug(LOG_TAG, "Removing download and notification id=" + downloadId);
        AsyncTaskUtils.execute(LOG_TAG, new DownloadManagerRemoveTask(mContext, downloadId));
    }

    @WorkerThread
    synchronized void onRequest(DownloadManagerRequestTask task) {

        /* Download file. */
        Uri downloadUrl = mReleaseDetails.getDownloadUrl();
        AppCenterLog.debug(LOG_TAG, "Start downloading new release from " + downloadUrl);
        DownloadManager downloadManager = getDownloadManager();
        DownloadManager.Request request = new DownloadManager.Request(downloadUrl);

        /* Hide mandatory download to prevent canceling via notification cancel or download UI delete. */
        if (mReleaseDetails.isMandatoryUpdate()) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setVisibleInDownloadsUi(false);
        }
        long enqueueTime = System.currentTimeMillis();
        long downloadId = downloadManager.enqueue(request);

        /* Check for if state changed and task not canceled in time. */
        if (mRequestTask == task) {

            /* Delete previous download. */
            long previousDownloadId = SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
            if (previousDownloadId != INVALID_DOWNLOAD_IDENTIFIER) {
                AppCenterLog.debug(LOG_TAG, "Delete previous download id=" + previousDownloadId);
                downloadManager.remove(previousDownloadId);
            }

            /* Store new download identifier. */
            setDownloadId(downloadId);
            mListener.onStart(enqueueTime);

            /* Start monitoring progress for mandatory update. */
            if (mReleaseDetails.isMandatoryUpdate()) {
                update();
            }
        } else {

            /* State changed quickly, cancel download. */
            AppCenterLog.debug(LOG_TAG, "State changed while downloading, cancel id=" + downloadId);
            downloadManager.remove(downloadId);
        }
    }

    @WorkerThread
    synchronized void onUpdate() {

        /* Query download manager. */
        DownloadManager downloadManager = getDownloadManager();
        try {
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
                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long currentSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    if (mListener.onProgress(currentSize, totalSize)) {

                        /* Schedule the next check if more updates are needed. */
                        HandlerUtils.getMainHandler().postAtTime(new Runnable() {

                            @Override
                            public void run() {
                                update();
                            }
                        }, HANDLER_TOKEN_CHECK_PROGRESS, SystemClock.uptimeMillis() + CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS);
                    }
                    return;
                }

                /* Complete download. */
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
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId, e);
            mListener.onError(e.getMessage());
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static Uri getFileUriOnOldDevices(Cursor cursor) throws IllegalArgumentException {
        return Uri.parse("file://" + cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)));
    }
}
