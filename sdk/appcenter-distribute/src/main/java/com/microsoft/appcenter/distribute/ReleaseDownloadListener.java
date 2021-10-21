/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private final String mOutputStreamName = "AppCenterPackageInstallerStream";

    private final int mBufferCapacity = 16384;

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

                    /*
                     * This start call triggers strict mode in UI thread so it
                     * needs to be done here without synchronizing
                     * (not to block methods waiting on synchronized on UI thread)
                     * so yes we could launch install and SDK being disabled.
                     *
                     * This corner case cannot be avoided without triggering
                     * strict mode exception.
                     */
                    AppCenterLog.info(LOG_TAG, "Show install UI.");
                    ParcelFileDescriptor pfd;
                    try {
                        pfd = getDownloadManager().openDownloadedFile(downloadId);
                        InputStream data = new FileInputStream(pfd.getFileDescriptor());
                        installPackage(data);
                    } catch (FileNotFoundException e) {
                        AppCenterLog.error(AppCenterLog.LOG_TAG, "Can't read data due to file not found. " + e.getMessage());
                    } catch (IOException e) {
                        AppCenterLog.error(AppCenterLog.LOG_TAG, "Update can't be installed due to error: " + e.getMessage());
                    }
                    Distribute.getInstance().setInstalling(mReleaseDetails);
                }
            }
        });
        return true;
    }

    private DownloadManager getDownloadManager() {
        return (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
    }

    /**
     * Install new release.
     * @param data input stream data from the install apk.
     * @throws IOException
     */
    private void installPackage(InputStream data)
            throws IOException {

        PackageInstaller.Session session = null;
        try {

            // Prepare package installer.
            PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);

            // Prepare session.
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);

            // Start installing.
            OutputStream out = session.openWrite(mOutputStreamName, 0, -1);
            byte[] buffer = new byte[mBufferCapacity];
            int c;
            while ((c = data.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            data.close();
            out.close();
            session.commit(createIntentSender(mContext, sessionId));
        } catch (IOException e) {
            AppCenterLog.error(LOG_TAG, "Couldn't install package", e);
        } catch (RuntimeException e) {
            if (session != null) {
                session.abandon();
            }
            AppCenterLog.error(LOG_TAG, "Couldn't install package", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Return IntentSender with the receiver that will be launched after installation.
     * @param context context.
     * @param sessionId install sessionId.
     * @return IntentSender with receiver.
     */
    private IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(AppCenterPackageInstallerReceiver.START_INTENT),
                0);
        return pendingIntent.getIntentSender();
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
