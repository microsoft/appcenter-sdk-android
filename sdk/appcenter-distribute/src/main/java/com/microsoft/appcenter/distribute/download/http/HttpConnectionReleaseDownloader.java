/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.distribute.PermissionUtils;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import java.util.ArrayList;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

/**
 * Downloads new releases on Android SDK versions prior to Lollipop.
 * Uses HttpConnection and AsyncTasks.
 */
public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    private Context mContext;

    public HttpConnectionReleaseDownloader(Context context) {
        mContext = context;
    }

    @Override
    public void download(ReleaseDetails releaseDetails, @NonNull Listener listener) {
        if (!prepareDownload(listener)) {
            return;
        }
        AsyncTaskUtils.execute(LOG_TAG, new HttpDownloadFileTask(releaseDetails, listener, mContext));
    }

    @Override
    public void delete() {
        AsyncTaskUtils.execute(LOG_TAG, new HttpRemoveDownloadTask(mContext));
    }

    private boolean prepareDownload(Listener listener) {
        if (!NetworkStateHelper.getSharedInstance(mContext).isNetworkConnected()) {
            listener.onError("No network connection, abort downloading.");
            return false;
        }
        String[] permissions = requiredPermissions();
        int[] permissionsState = PermissionUtils.permissionsState(mContext, permissions);
        if (!PermissionUtils.permissionsAreGranted(permissionsState)) {
            listener.onError("No external storage permission.");
            return false;
        }
        return true;
    }

    private static String[] requiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }
}
