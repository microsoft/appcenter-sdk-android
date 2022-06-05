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
    private final Deque<ReleaseInstaller> mInstallers = new LinkedList<>();

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

        /* Initialize installers list. Use separate thread for background operations. */
        HandlerThread thread = new HandlerThread("AppCenter.Installer");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        mInstallers.add(new SessionReleaseInstaller(context, handler, this));
        mInstallers.add(new IntentReleaseInstaller(context, handler, this));
        mCurrentInstaller = popNextInstaller();
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
        // Show mandatory dialog if cancelled in background.
        if (mCancelled && mReleaseDetails.isMandatoryUpdate()) {
            Distribute.getInstance().showMandatoryDownloadReadyDialog();
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
            Distribute.getInstance().showMandatoryDownloadReadyDialog();
        } else {
            Distribute.getInstance().completeWorkflow(mReleaseDetails);
        }
    }

    /**
     * Message to filter MIUI error to use fallback installer.
     */
    private static final String RECOVERABLE_ERROR = "INSTALL_FAILED_INTERNAL_ERROR: Permission Denied";

    private static boolean isRecoverableError(String errorMessage) {
        return RECOVERABLE_ERROR.equals(errorMessage);
    }
}
