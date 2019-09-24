/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.os.Build;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import java.util.NoSuchElementException;
import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.download.DownloadUtils.CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS;
import static com.microsoft.appcenter.distribute.download.DownloadUtils.HANDLER_TOKEN_CHECK_PROGRESS;
import static com.microsoft.appcenter.distribute.download.DownloadUtils.PREFERENCE_KEY_STORE_DOWNLOADING_RELEASE_APK_FILE;

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
     * Release details.
     */
    private ReleaseDetails mReleaseDetails;

    /**
     * Listener for download states.
     */
    private ReleaseDownloader.Listener mListener;

    /**
     * TODO desc
     */
    private ReleaseDownloader mManager;

    /**
     * Init.
     *
     * @param context        context.
     * @param downloadId     download identifier.
     * @param releaseDetails release details.
     */
    public CheckDownloadTask(Context context, long downloadId, ReleaseDetails releaseDetails, ReleaseDownloader manager, ReleaseDownloader.Listener listener) {
        mContext = context.getApplicationContext();
        mDownloadId = downloadId;
        mReleaseDetails = releaseDetails;
        mListener = listener;
        mManager = manager;
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
                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long currentSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    AppCenterLog.verbose(LOG_TAG, "currentSize=" + currentSize + " totalSize=" + totalSize);
                    return new DownloadProgress(currentSize, totalSize);
                }
                String localUri;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME));
                } else {
                    localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                }
                SharedPreferencesManager.putString(PREFERENCE_KEY_STORE_DOWNLOADING_RELEASE_APK_FILE, localUri);
                mListener.onComplete(localUri, mReleaseDetails);
                return null;
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to download update id=" + mDownloadId, e);
            mManager.delete();

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
                    mListener.onProgress(result.getTotalSize(), result.getCurrentSize());

                    /* And schedule the next check. */
                    if (mReleaseDetails.isMandatoryUpdate()) {
                        HandlerUtils.getMainHandler().postAtTime(new Runnable() {

                            @Override
                            public void run() {
                                mManager.download(mReleaseDetails, mListener);
                            }
                        }, HANDLER_TOKEN_CHECK_PROGRESS, SystemClock.uptimeMillis() + CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS);
                    }
                }
            });
        }
    }
}
