package com.microsoft.appcenter.distribute.install.session;

import static android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.install.AbstractReleaseInstaller;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SessionReleaseInstaller extends AbstractReleaseInstaller {

    /**
     * Name of package installer stream.
     */
    private static final String sOutputStreamName = "AppCenterPackageInstallerStream";

    /**
     * Buffer capacity of package installer.
     */
    private static final int BUFFER_CAPACITY = 64 * 1024;

    private static final int INVALID_SESSION_ID = -1;

    private BroadcastReceiver mInstallStatusReceiver;

    private PackageInstaller.SessionCallback mSessionCallback;

    private boolean mUserConfirmationRequested;

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
                startInstallSession(localUri);
            }
        });
    }

    @Override
    public void resume() {
        postDelayed(new Runnable() {

            @Override
            public void run() {
                delayedResume();
            }
        }, 500);
    }

    private synchronized void delayedResume() {
        if (mUserConfirmationRequested) {
            onCancel();
        }

        // Sometimes progress event comes a bit late, in this case second resume means cancellation.
        mUserConfirmationRequested = true;
    }

    @Override
    public synchronized void clear() {
        unregisterListeners();
        abandonSession();
    }

    @NonNull
    @Override
    public String toString() {
        return "PackageInstaller";
    }

    synchronized void onInstallProgress() {
        mUserConfirmationRequested = false;
    }

    synchronized void onInstallConfirmation(Intent intent) {
        AppCenterLog.info(LOG_TAG, "Ask confirmation to install a new release.");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mUserConfirmationRequested = true;
        post(new Runnable() {
            @Override
            public void run() {
                mContext.startActivity(intent);
            }
        });
    }

    synchronized void onInstallError(String message) {
        mSessionId = INVALID_SESSION_ID;
        onError(message);
    }

    synchronized void onInstallCancel() {
        mSessionId = INVALID_SESSION_ID;
        onCancel();
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

            // IllegalStateException - Too many active sessions
        } catch (IOException | IllegalStateException e) {
            if (session != null) {
                session.abandon();
            }
            onError("Couldn't install a new release: " + e);
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
            mContext.registerReceiver(mInstallStatusReceiver, InstallStatusReceiver.getInstallerReceiverFilter());
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
        return packageInstaller.openSession(mSessionId);
    }

    private void abandonSession() {
        if (mSessionId != INVALID_SESSION_ID) {
            PackageInstaller packageInstaller = getPackageInstaller();
            packageInstaller.abandonSession(mSessionId);
            mSessionId = INVALID_SESSION_ID;
        }
    }
}
