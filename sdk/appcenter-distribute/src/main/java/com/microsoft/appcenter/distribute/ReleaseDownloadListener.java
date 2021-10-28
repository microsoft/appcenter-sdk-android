/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import android.widget.Toast;

import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import java.text.NumberFormat;
import java.util.Locale;

import static com.microsoft.appcenter.distribute.DistributeConstants.HANDLER_TOKEN_CHECK_PROGRESS;
import static com.microsoft.appcenter.distribute.DistributeConstants.KIBIBYTE_IN_BYTES;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.MEBIBYTE_IN_BYTES;

/**
 * Listener for downloading progress.
 */
class ReleaseDownloadListener implements ReleaseDownloader.Listener {

    /**
     * Context.
     */
    private final Context mContext;

    /**
     * Private field to store information about release we are currently working with.
     */
    private final ReleaseDetails mReleaseDetails;

    ReleaseDownloadListener(@NonNull Context context, @NonNull ReleaseDetails releaseDetails) {
        mContext = context;
        mReleaseDetails = releaseDetails;
    }

    /**
     * Last download progress dialog that was shown.
     * Android 8 deprecates this dialog but only reason is that they want us to use a non modal
     * progress indicator while we actually use it to be a modal dialog for forced update.
     * They will always keep this dialog to remain compatible but just mark it deprecated.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private android.app.ProgressDialog mProgressDialog;

    @WorkerThread
    @Override
    public void onStart(final long enqueueTime) {
        AppCenterLog.debug(LOG_TAG, String.format(Locale.ENGLISH, "Start download %s (%d) update.",
                mReleaseDetails.getShortVersion(), mReleaseDetails.getVersion()));

        // Run on the UI thread to prevent deadlock.
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Distribute.getInstance().setDownloading(mReleaseDetails, enqueueTime);
            }
        });
    }

    @WorkerThread
    @Override
    public synchronized boolean onProgress(final long currentSize, final long totalSize) {
        AppCenterLog.verbose(LOG_TAG, String.format(Locale.ENGLISH, "Downloading %s (%d) update: %d KiB / %d KiB",
                mReleaseDetails.getShortVersion(), mReleaseDetails.getVersion(),
                currentSize / KIBIBYTE_IN_BYTES, totalSize / KIBIBYTE_IN_BYTES));
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateProgressDialog(currentSize, totalSize);
            }
        });
        return mProgressDialog != null;
    }

    @WorkerThread
    @Override
    public boolean onComplete(@NonNull final Long downloadId) {
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                /* Check if app should install now. */
                if (!Distribute.getInstance().notifyDownload(mReleaseDetails)) {
                    AppCenterLog.info(LOG_TAG, "Download was completed. Start to install a new release update.");
                    Distribute.getInstance().setInstalling(mReleaseDetails);
                    Distribute.getInstance().showSystemSettingsDialogOrStartInstalling(downloadId);
                }
            }
        });
        return true;
    }

    @WorkerThread
    @Override
    public void onError(@Nullable String errorMessage) {
        AppCenterLog.error(LOG_TAG, String.format(Locale.ENGLISH, "Failed to download %s (%d) update: %s",
                mReleaseDetails.getShortVersion(), mReleaseDetails.getVersion(), errorMessage));

        // Run on the UI thread to prevent deadlock.
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mContext, R.string.appcenter_distribute_downloading_error, Toast.LENGTH_SHORT).show();
                Distribute.getInstance().completeWorkflow(mReleaseDetails);
            }
        });
    }

    /**
     * Show download progress. Only used for mandatory updates.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    synchronized android.app.ProgressDialog showDownloadProgress(Activity foregroundActivity) {
        if (!mReleaseDetails.isMandatoryUpdate()) {
            return null;
        }
        mProgressDialog = new android.app.ProgressDialog(foregroundActivity);
        mProgressDialog.setTitle(R.string.appcenter_distribute_downloading_update);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressNumberFormat(null);
        mProgressDialog.setProgressPercentFormat(null);
        return mProgressDialog;
    }

    /**
     * Hide progress dialog and stop updating. Only used for mandatory updates.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    synchronized void hideProgressDialog() {
        if (mProgressDialog != null) {
            final android.app.ProgressDialog progressDialog = mProgressDialog;
            mProgressDialog = null;

            /* This can be called from background check download task. */
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    progressDialog.cancel();
                }
            });
            HandlerUtils.getMainHandler().removeCallbacksAndMessages(HANDLER_TOKEN_CHECK_PROGRESS);
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @UiThread
    private synchronized void updateProgressDialog(final long currentSize, final long totalSize) {

        /* If file size is known update downloadProgress bar. */
        if (mProgressDialog != null && totalSize >= 0) {

            /* When we switch from indeterminate to determinate */
            if (mProgressDialog.isIndeterminate()) {

                /* Configure the progress dialog determinate style. */
                mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                mProgressDialog.setProgressNumberFormat(mContext.getString(R.string.appcenter_distribute_download_progress_number_format));
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax((int) (totalSize / MEBIBYTE_IN_BYTES));
            }
            mProgressDialog.setProgress((int) (currentSize / MEBIBYTE_IN_BYTES));
        }
    }
}
