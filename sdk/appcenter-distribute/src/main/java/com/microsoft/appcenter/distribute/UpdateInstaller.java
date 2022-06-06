/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.distribute.install.ReleaseInstaller;
import com.microsoft.appcenter.distribute.install.intent.IntentReleaseInstaller;
import com.microsoft.appcenter.distribute.install.session.SessionReleaseInstaller;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Installer of downloaded package with awareness of service state and fallback mechanism.
 */
class UpdateInstaller implements ReleaseInstaller, ReleaseInstaller.Listener {

    /**
     * Store information about release we are currently working with.
     */
    private final ReleaseDetails mReleaseDetails;

    /**
     * Queue of available to use installers.
     */
    private final Deque<ReleaseInstaller> mInstallers;

    /**
     * The installer that we currently use.
     */
    private ReleaseInstaller mCurrentInstaller;

    /**
     * Keep Uri of downloaded package to pass it in case of using fallback.
     */
    private Uri mLocalUri;

    /**
     * The flag to track the cancellation event.
     */
    private boolean mCancelled;

    UpdateInstaller(Context context, ReleaseDetails releaseDetails) {
        mReleaseDetails = releaseDetails;
        mInstallers = createInstallers(context);
        mCurrentInstaller = popNextInstaller();
    }

    @VisibleForTesting
    UpdateInstaller(ReleaseDetails releaseDetails, Deque<ReleaseInstaller> installers) {
        mReleaseDetails = releaseDetails;
        mInstallers = installers;
        mCurrentInstaller = popNextInstaller();
    }

    private Deque<ReleaseInstaller> createInstallers(Context context) {

        /* Initialize installers list. Use separate thread for background operations. */
        Handler handler = createInstallerHandler();
        Deque<ReleaseInstaller> installers = new LinkedList<>();
        installers.add(new SessionReleaseInstaller(context, handler, this));
        installers.add(new IntentReleaseInstaller(context, handler, this));
        return installers;
    }

    private ReleaseInstaller popNextInstaller() {
        if (mInstallers.size() == 0) {
            return null;
        }
        ReleaseInstaller nextInstaller = mInstallers.pop();
        AppCenterLog.debug(LOG_TAG, "Trying to install update via " + nextInstaller.toString() + ".");
        return nextInstaller;
    }

    @Override
    public synchronized void install(@NonNull Uri localUri) {
        mCancelled = false;
        mLocalUri = localUri;
        if (mCurrentInstaller != null) {
            mCurrentInstaller.install(localUri);
        }
    }

    /**
     * Process resume distribute workflow.
     */
    public synchronized void resume() {

        /* Show mandatory dialog if cancelled in background. */
        if (mCancelled && mReleaseDetails.isMandatoryUpdate()) {
            Distribute.getInstance().showMandatoryDownloadReadyDialog(mReleaseDetails);
        }
    }

    @Override
    public synchronized void clear() {
        if (mCurrentInstaller != null) {
            mCurrentInstaller.clear();
        }
    }

    @Override
    public synchronized void onError(String errorMessage) {
        if (isRecoverableError(errorMessage)) {
            mCurrentInstaller.clear();
            mCurrentInstaller = popNextInstaller();
            if (mCurrentInstaller != null) {
                mCurrentInstaller.install(mLocalUri);
                return;
            }
        }
        Distribute.getInstance().completeWorkflow(mReleaseDetails);
    }

    @Override
    public synchronized void onCancel() {
        mCancelled = true;
        if (mReleaseDetails.isMandatoryUpdate()) {
            Distribute.getInstance().showMandatoryDownloadReadyDialog(mReleaseDetails);
        } else {
            Distribute.getInstance().completeWorkflow(mReleaseDetails);
        }
    }

    private static Handler createInstallerHandler() {
        HandlerThread thread = new HandlerThread("AppCenter.Installer");
        thread.start();
        return new Handler(thread.getLooper());
    }

    /**
     * Message to filter MIUI error to use fallback installer.
     */
    @VisibleForTesting
    static final String RECOVERABLE_ERROR = "INSTALL_FAILED_INTERNAL_ERROR: Permission Denied";

    private static boolean isRecoverableError(String errorMessage) {
        return RECOVERABLE_ERROR.equals(errorMessage);
    }
}
