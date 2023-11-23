/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.session;

import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.install.AbstractReleaseInstaller;
import com.microsoft.appcenter.distribute.install.ReleaseInstallerActivity;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Installer based on {@link PackageInstaller.Session} API.
 */
public class SessionReleaseInstaller extends AbstractReleaseInstaller {

    /**
     * Name of package installer stream.
     */
    private static final String sOutputStreamName = "AppCenterPackageInstallerStream";

    /**
     * Buffer capacity of package installer.
     */
    private static final int BUFFER_CAPACITY = 64 * 1024;

    /**
     * {@link PackageInstaller.SessionInfo#INVALID_ID} requires requires API level 29,
     * so use our own constant.
     */
    private static final int INVALID_SESSION_ID = -1;

    /**
     * Timeout to check cancellation state in milliseconds.
     */
    private static final long CANCEL_TIMEOUT = 1000;

    /**
     * Install status receiver. Keep the reference to unsubscribe it.
     */
    private BroadcastReceiver mInstallStatusReceiver;

    /**
     * Install session callback. Keep the reference to unsubscribe it.
     */
    private PackageInstaller.SessionCallback mSessionCallback;

    /**
     * Tracking if user confirmation was requested.
     * Needed to cancel the process in case of missing system callback.
     */
    private boolean mUserConfirmationRequested;

    /**
     * Current install session id.
     */
    private int mSessionId = INVALID_SESSION_ID;

    public SessionReleaseInstaller(Context context, Handler installerHandler, Listener listener) {
        super(context, installerHandler, listener);
    }

    private PackageInstaller getPackageInstaller() {
        return mContext.getPackageManager().getPackageInstaller();
    }

    @AnyThread
    @Override
    public void install(@NonNull Uri localUri) {
        registerListeners();
        post(new Runnable() {

            @Override
            public void run() {

                /* Abandon previous session if needed. */
                abandonSession();

                /* Start install session from background thread. */
                startInstallSession(localUri);
            }
        });
    }

    @Override
    public synchronized void clear() {
        super.clear();
        unregisterListeners();
        abandonSession();
    }

    @NonNull
    @Override
    public String toString() {
        return "PackageInstaller";
    }

    synchronized void onInstallProgress(int sessionId) {
        if (mSessionId != sessionId) {
            return;
        }

        /*
         * Reset confirmation flag.
         * Any progress event during user confirmation means that installation is not cancelled.
         * The only reason to track it is that system dialog might be closed by clicking
         * outside. In this case, Android doesn't produce any event, so we have to use it
         * as workaround to mark installation as cancelled and re-try in case of mandatory update.
         */
        mUserConfirmationRequested = false;
    }

    synchronized void onInstallConfirmation(int sessionId, Intent confirmIntent) {
        if (mSessionId != sessionId) {
            return;
        }
        AppCenterLog.info(LOG_TAG, "Ask confirmation to install a new release.");
        mUserConfirmationRequested = true;

        /* Use proxy activity to handle closing event. */
        AppCenterFuture<ReleaseInstallerActivity.Result> confirmFuture = ReleaseInstallerActivity.startActivityForResult(mContext, confirmIntent);
        if (confirmFuture == null) {

            /* Another installing activity already in progress. Precaution for unexpected case. */
            return;
        }
        confirmFuture.thenAccept(new AppCenterConsumer<ReleaseInstallerActivity.Result>() {

            @Override
            public void accept(ReleaseInstallerActivity.Result result) {

                /* Check for progress events after closing confirmation dialog. */
                cancelIfNoProgress();
            }
        });
    }

    synchronized void onInstallError(int sessionId, String message) {
        if (mSessionId != sessionId) {
            return;
        }
        mSessionId = INVALID_SESSION_ID;
        onError(message);
    }

    synchronized void onInstallCancel(int sessionId) {
        if (mSessionId != sessionId) {
            return;
        }
        mSessionId = INVALID_SESSION_ID;
        onCancel();
    }

    private void cancelIfNoProgress() {

        /*
         * In some cases progress event might be delayed a bit.
         * Use threshold timeout to prevent false-alarming cancelling.
         */
        postDelayed(new Runnable() {

            @Override
            public void run() {

                /* Cancels installation if this flag hasn't been reset by progress event. */
                if (mUserConfirmationRequested) {
                    AppCenterLog.error(LOG_TAG, "Canceling installation due to lack of progress.");
                    onCancel();
                }
            }
        }, CANCEL_TIMEOUT);
    }

    @WorkerThread
    private synchronized void startInstallSession(@NonNull Uri localUri) {
        PackageInstaller.Session session = null;
        try (ParcelFileDescriptor fileDescriptor = mContext.getContentResolver().openFileDescriptor(localUri, "r")) {

            /* Prepare session. */
            session = createSession(fileDescriptor);
            addFileToInstallSession(fileDescriptor, session);

            /* Start to install a new release. */
            IntentSender statusReceiver = InstallStatusReceiver.getInstallStatusIntentSender(mContext, mSessionId);
            session.commit(statusReceiver);
            session.close();
        } catch (IOException | RuntimeException | NoSuchFieldException | IllegalAccessException e) {
            if (session != null) {
                session.abandon();
            }
            onError("Cannot initiate PackageInstaller.Session", e);
        }
    }

    @WorkerThread
    private static void addFileToInstallSession(ParcelFileDescriptor fileDescriptor, PackageInstaller.Session session)
            throws IOException {
        try (OutputStream out = session.openWrite(sOutputStreamName, 0, fileDescriptor.getStatSize());
             InputStream in = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            byte[] buffer = new byte[BUFFER_CAPACITY];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            session.fsync(out);
        }
    }

    private synchronized void registerListeners() {
        if (mInstallStatusReceiver == null) {
            AppCenterLog.debug(LOG_TAG, "Register receiver for installing a new release.");
            mInstallStatusReceiver = new InstallStatusReceiver(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mContext.registerReceiver(mInstallStatusReceiver, InstallStatusReceiver.getInstallerReceiverFilter(), Context.RECEIVER_EXPORTED);
            } else {
                mContext.registerReceiver(mInstallStatusReceiver, InstallStatusReceiver.getInstallerReceiverFilter());
            }
        }
        if (mSessionCallback == null) {
            PackageInstaller packageInstaller = getPackageInstaller();
            mSessionCallback = new PackageInstallerListener(this);
            packageInstaller.registerSessionCallback(mSessionCallback);
        }
    }

    private synchronized void unregisterListeners() {
        if (mInstallStatusReceiver != null) {
            AppCenterLog.debug(LOG_TAG, "Unregister receiver for installing a new release.");
            mContext.unregisterReceiver(mInstallStatusReceiver);
            mInstallStatusReceiver = null;
        }
        if (mSessionCallback != null) {
            PackageInstaller packageInstaller = getPackageInstaller();
            packageInstaller.unregisterSessionCallback(mSessionCallback);
            mSessionCallback = null;
        }
    }

    private PackageInstaller.Session createSession(ParcelFileDescriptor fileDescriptor) throws IOException {
        PackageInstaller packageInstaller = getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(MODE_FULL_INSTALL);
        params.setSize(fileDescriptor.getStatSize());
        params.setAppPackageName(mContext.getPackageName());
        mSessionId = packageInstaller.createSession(params);
        try {
            return packageInstaller.openSession(mSessionId);

            /* IllegalStateException might be thrown in case of too many active sessions. */
        } catch (IllegalStateException e) {
            AppCenterLog.warn(LOG_TAG, "Cannot open session, trying to cleanup previous ones.", e);

            /* Trying to clean up leaked sessions that prevent opening new ones. */
            cleanPreviousSessions();

            /* Second try to open install session. */
            return packageInstaller.openSession(mSessionId);
        }
    }

    private void abandonSession() {
        if (mSessionId != INVALID_SESSION_ID) {
            AppCenterLog.debug(LOG_TAG, "Abandon PackageInstaller session.");
            PackageInstaller packageInstaller = getPackageInstaller();
            packageInstaller.abandonSession(mSessionId);
            mSessionId = INVALID_SESSION_ID;
        }
    }

    private void cleanPreviousSessions() {
        PackageInstaller packageInstaller = getPackageInstaller();
        for (PackageInstaller.SessionInfo session: getPackageInstaller().getMySessions()) {
            AppCenterLog.warn(LOG_TAG, "Abandon leaked session: " + session.getSessionId());
            packageInstaller.abandonSession(session.getSessionId());
        }
    }
}
