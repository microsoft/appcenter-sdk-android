/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.PermissionUtils;
import com.microsoft.appcenter.distribute.R;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.AbstractReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.io.File;
import java.util.ArrayList;

import static com.microsoft.appcenter.distribute.DistributeConstants.KIBIBYTE_IN_BYTES;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;

/**
 * Downloads new releases directly via HttpsURLConnection for Android versions prior to 5.0.
 */
public class HttpConnectionReleaseDownloader extends AbstractReleaseDownloader {

    public HttpConnectionReleaseDownloader(@NonNull Context context, @NonNull ReleaseDetails releaseDetails, @NonNull Listener listener) {
        super(context, releaseDetails, listener);
    }

    /**
     * A file that used to write downloaded package.
     */
    private File mTargetFile;

    /**
     * Progress notification builder.
     * It must be stored to avoid notification flickering.
     */
    private Notification.Builder mNotificationBuilder;

    private HttpConnectionCheckTask mCheckTask;

    private HttpConnectionDownloadFileTask mDownloadTask;

    @Nullable
    @WorkerThread
    File getTargetFile() {
        if (mTargetFile == null) {
            File downloadsDirectory = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsDirectory != null) {
                mTargetFile = new File(downloadsDirectory, mReleaseDetails.getReleaseHash() + ".apk");
            }
        }
        return mTargetFile;
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @WorkerThread
    synchronized String getDownloadedReleaseFilePath() {
        return SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null);
    }

    @WorkerThread
    synchronized void setDownloadedReleaseFilePath(String downloadedReleaseFilePath) {
        if (isCancelled()) {
            return;
        }
        if (downloadedReleaseFilePath != null) {
            SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, downloadedReleaseFilePath);
        } else {
            SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE);
        }
    }

    /**
     * Get progress notification builder.
     *
     * @see #mNotificationBuilder
     */
    @VisibleForTesting
    @NonNull
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    Notification.Builder getNotificationBuilder() {
        if (mNotificationBuilder == null) {
            mNotificationBuilder = new Notification.Builder(mContext);
        }
        return mNotificationBuilder;
    }

    @Override
    public synchronized boolean isDownloading() {
        return mDownloadTask != null;
    }

    @AnyThread
    @Override
    public synchronized void resume() {
        if (isCancelled()) {
            return;
        }
        if (!NetworkStateHelper.getSharedInstance(mContext).isNetworkConnected()) {
            mListener.onError("No network connection, abort downloading.");
            return;
        }
        String[] permissions = requiredPermissions();
        int[] permissionsState = PermissionUtils.permissionsState(mContext, permissions);
        if (!PermissionUtils.permissionsAreGranted(permissionsState)) {
            mListener.onError("No external storage permission.");
            return;
        }

        /* Do all file-system related checks in the background. */
        check();
    }

    @Override
    public synchronized void cancel() {
        if (isCancelled()) {
            return;
        }
        super.cancel();
        if (mCheckTask != null) {
            mCheckTask.cancel(true);
            mCheckTask = null;
        }
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        String filePath = getDownloadedReleaseFilePath();
        if (filePath != null) {
            removeFile(new File(filePath));
            setDownloadedReleaseFilePath(null);
        }
        cancelProgressNotification();
    }

    private synchronized void check() {
        mCheckTask = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionCheckTask(this));
    }

    private synchronized void downloadFile(File file) {
        if (mDownloadTask != null) {
            AppCenterLog.debug(LOG_TAG, "Downloading of " + file.getPath() + " is already in progress.");
            return;
        }
        Uri downloadUrl = mReleaseDetails.getDownloadUrl();
        AppCenterLog.debug(LOG_TAG, "Start downloading new release from " + downloadUrl);
        mDownloadTask = AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionDownloadFileTask(this, downloadUrl, file));
    }

    private void removeFile(File file) {
        AppCenterLog.debug(LOG_TAG, "Removing downloaded file from " + file.getAbsolutePath());
        AsyncTaskUtils.execute(LOG_TAG, new HttpConnectionRemoveFileTask(file));
    }

    private void showProgressNotification(long currentSize, long totalSize) {
        if (mReleaseDetails.isMandatoryUpdate()) {
            return;
        }
        Notification.Builder builder = getNotificationBuilder();
        builder.setContentTitle(mContext.getString(R.string.appcenter_distribute_downloading_update))
                .setSmallIcon(mContext.getApplicationInfo().icon)
                .setProgress((int) (totalSize / KIBIBYTE_IN_BYTES), (int) (currentSize / KIBIBYTE_IN_BYTES), totalSize <= 0);
        getNotificationManager().notify(getNotificationId(), builder.build());
    }

    private void cancelProgressNotification() {
        getNotificationManager().cancel(getNotificationId());
    }

    @WorkerThread
    synchronized void onStart(File targetFile) {
        if (isCancelled()) {
            return;
        }
        downloadFile(targetFile);
    }

    @WorkerThread
    synchronized void onDownloadStarted(long enqueueTime) {
        if (isCancelled()) {
            return;
        }
        showProgressNotification(0, 0);
        mListener.onStart(enqueueTime);
    }

    @WorkerThread
    synchronized void onDownloadProgress(final long currentSize, final long totalSize) {
        if (isCancelled()) {
            return;
        }
        showProgressNotification(currentSize, totalSize);
        mListener.onProgress(currentSize, totalSize);
    }

    @WorkerThread
    synchronized void onDownloadComplete(File targetFile) {
        if (isCancelled()) {
            return;
        }
        cancelProgressNotification();

        /* Check downloaded file size. */
        if (mReleaseDetails.getSize() != targetFile.length()) {
            mListener.onError("Downloaded file has incorrect size.");
            return;
        }

        /* Store that the release file has been downloaded. */
        String downloadedReleaseFilePath = targetFile.getAbsolutePath();
        setDownloadedReleaseFilePath(downloadedReleaseFilePath);
        mListener.onComplete(Uri.parse("file://" + downloadedReleaseFilePath));
    }

    @WorkerThread
    synchronized void onDownloadError(String errorMessage) {
        if (isCancelled()) {
            return;
        }
        cancelProgressNotification();
        mListener.onError(errorMessage);
    }

    @VisibleForTesting
    static String[] requiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }

    /**
     * Get the notification identifier for displaying progress.
     *
     * @return notification identifier for displaying progress.
     */
    private static int getNotificationId() {
        return HttpConnectionReleaseDownloader.class.getName().hashCode();
    }
}
