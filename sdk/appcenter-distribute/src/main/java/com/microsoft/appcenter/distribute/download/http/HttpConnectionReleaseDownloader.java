/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.distribute.DistributeConstants;
import com.microsoft.appcenter.distribute.PermissionUtils;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.io.File;
import java.util.ArrayList;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;

/**
 * Downloads new releases on Android SDK versions prior to Lollipop.
 * Uses HttpConnection and AsyncTasks.
 */
public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private final Context mContext;

    private final ReleaseDetails mReleaseDetails;

    private final ReleaseDownloader.Listener mListener;

    private File mTargetFile;

    public HttpConnectionReleaseDownloader(@NonNull Context context, @NonNull ReleaseDetails releaseDetails, @NonNull ReleaseDownloader.Listener listener) {
        mContext = context;
        mReleaseDetails = releaseDetails;
        mListener = listener;
    }

    private File getTargetFile() {
        if (mTargetFile == null) {
            mTargetFile = new File(DistributeConstants.getDownloadFilesPath(mContext), mReleaseDetails.getReleaseHash() + ".apk");
        }
        return mTargetFile;
    }

    @Override
    public void resume() {
        File targetFile = getTargetFile();

        /* Check if we have already downloaded the release. */
        String downloadedReleaseFilePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null);
        if (downloadedReleaseFilePath != null) {

            /* Check if it's the same release. */
            if (downloadedReleaseFilePath.equals(targetFile.getAbsolutePath())) {
                File downloadedReleaseFile = new File(downloadedReleaseFilePath);
                if (downloadedReleaseFile.exists()) {
                    onDownloadComplete(targetFile);
                    return;
                }
            } else {

                /* Remove previously downloaded release. */
                mContext.deleteFile(downloadedReleaseFilePath);
                SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE);
            }
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
        long enqueueTime = System.currentTimeMillis();
        AsyncTaskUtils.execute(LOG_TAG, new HttpDownloadFileTask(this, mReleaseDetails.getDownloadUrl(), targetFile));
        mListener.onStart(enqueueTime);
    }

    @Override
    public void delete() {
        String filePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null);
        if (filePath != null) {
            AsyncTaskUtils.execute(LOG_TAG, new RemoveFileTask(mContext, filePath));
            SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE);
        }
    }

    void onDownloadProgress(final long currentSize, final long totalSize) {
        mListener.onProgress(currentSize, totalSize);
    }

    void onDownloadComplete(File targetFile) {
        String downloadedReleaseFilePath = targetFile.getAbsolutePath();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, downloadedReleaseFilePath);
        mListener.onComplete(Uri.parse("file://" + downloadedReleaseFilePath));
    }

    void onDownloadError(String errorMessage) {
        mListener.onError(errorMessage);
    }

    private static String[] requiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }
}
