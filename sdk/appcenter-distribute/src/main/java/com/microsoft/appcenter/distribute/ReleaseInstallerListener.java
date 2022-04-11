/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInstaller;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.annotation.UiThread;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Listener for installation progress.
 */
public class ReleaseInstallerListener extends PackageInstaller.SessionCallback {

    /**
     * Total progress size.
     */
    private static final int TOTAL_PROGRESS_SIZE = 100;

    /**
     * Context.
     */
    private final Context mContext;

    /**
     * Download id.
     */
    private long mDownloadId;

    /**
     * Total size of the file.
     */
    private long mTotalSize;

    /**
     * Last download progress dialog that was shown.
     * Android 8 deprecates this dialog but only reason is that they want us to use a non modal
     * progress indicator while we actually use it to be a modal dialog for forced update.
     * They will always keep this dialog to remain compatible but just mark it deprecated.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private android.app.ProgressDialog mProgressDialog;

    public ReleaseInstallerListener(Context context) {
        mContext = context;
    }

    /**
     * Set the downloadId of the downloaded file to be installed.
     *
     * @param downloadId downloadId of the downloaded file.
     */
    public void setDownloadId(long downloadId) {
        mDownloadId = downloadId;
    }

    /**
     * Set the total size of the downloaded file.
     *
     * @param totalSize downloadId of the downloaded file.
     */
    public void setTotalSize(long totalSize) {
        mTotalSize = totalSize;
    }

    /**
     * Start to install a new release.
     */
    public void startInstall() {
        AppCenterLog.debug(AppCenterLog.LOG_TAG, "Start installing new release...");
        try {
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
            // FIXME: android.os.strictmode.LeakedClosableViolation: A resource was acquired at attached stack trace but never released.
            ParcelFileDescriptor fileDescriptor = downloadManager.openDownloadedFile(mDownloadId);
            if (fileDescriptor.getStatSize() != mTotalSize) {
                onInvalidFile();
                return;
            }
            InstallerUtils.installPackage(fileDescriptor, mContext, this);
        } catch (IOException e) {
            AppCenterLog.error(AppCenterLog.LOG_TAG, "Update can't be installed.", e);
        }
    }

    @Override
    public void onCreated(int sessionId) {
        AppCenterLog.debug(LOG_TAG, "The install session was created.");
    }

    @Override
    public void onBadgingChanged(int sessionId) {
    }

    @Override
    public void onActiveChanged(int sessionId, boolean active) {
        Distribute.getInstance().notifyInstallProgress(true);
    }

    @Override
    public void onProgressChanged(int sessionId, float progress) {
        final int downloadProgress = (int)(progress * 100);
        AppCenterLog.verbose(LOG_TAG, String.format(Locale.ENGLISH, "Installation progress: %d / 100.", downloadProgress));
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateInstallProgressDialog(downloadProgress);
            }
        });
    }

    @Override
    public void onFinished(int sessionId, final boolean success) {
        AppCenterLog.debug(LOG_TAG, String.format(Locale.ENGLISH,"The installation of the new version is completed with the result: %s.", success ? "successful" : "failure"));

        // Run on the UI thread to prevent deadlock.
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (!success) {
                    // FIXME: StrictMode policy violation: android.os.strictmode.IncorrectContextUseViolation
                    Toast.makeText(mContext, mContext.getString(R.string.appcenter_distribute_something_went_wrong_during_installing_new_release), Toast.LENGTH_SHORT).show();
                }
                Distribute.getInstance().notifyInstallProgress(false);
            }
        });
    }

    private void onInvalidFile() {
        AppCenterLog.error(AppCenterLog.LOG_TAG, "Failed to start installing new release. The file is invalid.");
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mContext, mContext.getString(R.string.appcenter_distribute_failed_file_during_install_update), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hide the install progress dialog.
     */
    public void hideInstallProgressDialog() {
        AppCenterLog.debug(LOG_TAG, "Hide the install progress dialog.");
        if (mProgressDialog != null) {
            final android.app.ProgressDialog progressDialog = mProgressDialog;
            mProgressDialog = null;

            /* This can be called from background check download task. */
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            });
        }
    }

    /**
     * Update progress on the install progress dialog.
     *
     * @param currentSize current progress status.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @UiThread
    private void updateInstallProgressDialog(final int currentSize) {
        AppCenterLog.debug(LOG_TAG, "Update the install progress dialog.");

        /* If file size is known update downloadProgress bar. */
        if (mProgressDialog != null) {

            /* When we switch from indeterminate to determinate */
            if (mProgressDialog.isIndeterminate()) {

                /* Configure the progress dialog determinate style. */
                mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                mProgressDialog.setProgressNumberFormat(mContext.getString(R.string.appcenter_distribute_install_progress_number_format));
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(TOTAL_PROGRESS_SIZE);
            }
            mProgressDialog.setProgress(currentSize);
        }
    }

    /**
     * Show the install progress dialog.
     *
     * @param foregroundActivity any activity.
     * @return install progress dialog.
     */
    @UiThread
    public Dialog showInstallProgressDialog(Activity foregroundActivity) {
        AppCenterLog.debug(LOG_TAG, "Show the install progress dialog.");
        mProgressDialog = new android.app.ProgressDialog(foregroundActivity);
        mProgressDialog.setTitle(mContext.getString(R.string.appcenter_distribute_install_dialog));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressNumberFormat(null);
        mProgressDialog.setProgressPercentFormat(null);
        return mProgressDialog;
    }
}
