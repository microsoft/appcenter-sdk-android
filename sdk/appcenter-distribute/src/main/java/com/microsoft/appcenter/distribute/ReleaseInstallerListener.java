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

import androidx.annotation.UiThread;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Listener for installation progress.
 */
public class ReleaseInstallerListener extends PackageInstaller.SessionCallback {

    /**
     * Context.
     */
    private final Context mContext;

    /**
     * Download id.
     */
    private long mDownloadId;

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

    public void setDownloadId(long downloadId) {
        mDownloadId = downloadId;
    }

    public void startInstall() {
        AppCenterLog.debug(AppCenterLog.LOG_TAG, "Start installing new release...");
        ParcelFileDescriptor pfd;
        try {
            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
            pfd = downloadManager.openDownloadedFile(mDownloadId);
            InputStream data = new FileInputStream(pfd.getFileDescriptor());
            InstallerUtils.installPackage(data, mContext, this);
        } catch (FileNotFoundException e) {
            AppCenterLog.error(AppCenterLog.LOG_TAG, "Can't read data due to file not found. " + e.getMessage());
        } catch (IOException e) {
            AppCenterLog.error(AppCenterLog.LOG_TAG, "Update can't be installed due to error: " + e.getMessage());
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
        final int totalSize = 100;
        final int downloadProgress = (int)(progress * 100);
        AppCenterLog.verbose(LOG_TAG, String.format(Locale.ENGLISH, "Installing %d of %d done.", downloadProgress, totalSize));
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateInstallProgressDialog(downloadProgress, totalSize);
            }
        });
    }

    @Override
    public void onFinished(int sessionId, boolean success) {
        AppCenterLog.debug(LOG_TAG, String.format(Locale.ENGLISH,"The installation of the new version is complete with the result: %s.", success ? "successful" : "failure"));

        // Run on the UI thread to prevent deadlock.
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Distribute.getInstance().notifyInstallProgress(false);
            }
        });
    }

    public synchronized void hideInstallProgressDialog() {
        AppCenterLog.debug(LOG_TAG, "Hide install progress dialog.");
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

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @UiThread
    private synchronized void updateInstallProgressDialog(final int currentSize, final int totalSize) {
        AppCenterLog.debug(LOG_TAG, "Show installation progress dialog.");

        /* If file size is known update downloadProgress bar. */
        if (mProgressDialog != null && totalSize >= 0) {

            /* When we switch from indeterminate to determinate */
            if (mProgressDialog.isIndeterminate()) {

                /* Configure the progress dialog determinate style. */
                mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                mProgressDialog.setProgressNumberFormat(mContext.getString(R.string.appcenter_distribute_install_progress_number_format));
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(totalSize);
            }
            mProgressDialog.setProgress(currentSize);
        }
    }

    @UiThread
    public synchronized Dialog showInstallProgressDialog(Activity foregroundActivity) {
        AppCenterLog.debug(LOG_TAG, "Show installation progress dialog.");
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
