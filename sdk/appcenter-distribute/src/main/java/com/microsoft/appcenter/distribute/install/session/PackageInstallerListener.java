/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.session;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.pm.PackageInstaller;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Locale;

/**
 * Listener for installation progress.
 */
public class PackageInstallerListener extends PackageInstaller.SessionCallback {

    private final SessionReleaseInstaller mInstaller;

    PackageInstallerListener(@NonNull SessionReleaseInstaller installer) {
        mInstaller = installer;
    }

    @WorkerThread
    @Override
    public void onCreated(int sessionId) {
        AppCenterLog.verbose(LOG_TAG, "The install session was created. sessionId=" + sessionId);
    }

    @WorkerThread
    @Override
    public void onBadgingChanged(int sessionId) {
    }

    @WorkerThread
    @Override
    public void onActiveChanged(int sessionId, boolean active) {
    }

    @WorkerThread
    @Override
    public void onProgressChanged(int sessionId, float progress) {
        final int downloadProgress = (int) (progress * 100);
        AppCenterLog.verbose(LOG_TAG, "Installation progress: " + downloadProgress + "%. sessionId=" + sessionId);
        mInstaller.onInstallProgress(sessionId);
    }

    @WorkerThread
    @Override
    public void onFinished(int sessionId, boolean success) {
        AppCenterLog.verbose(LOG_TAG, "The installation has been finished. sessionId=" + sessionId + ", success=" + success);
    }
}
