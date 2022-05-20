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

class UpdateInstaller implements ReleaseInstaller, ReleaseInstaller.Listener {
    private final Deque<ReleaseInstaller> mInstallers = new LinkedList<>();
    private final ReleaseDetails mReleaseDetails;
    private ReleaseInstaller mCurrent;
    private Uri mLocalUri;
    private boolean mCancelled;

    UpdateInstaller(Context context, ReleaseDetails releaseDetails) {
        HandlerThread thread = new HandlerThread("AppCenter.Installer");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        mInstallers.add(new SessionReleaseInstaller(context, handler, this));
        mInstallers.add(new IntentReleaseInstaller(context, handler, this));
        mReleaseDetails = releaseDetails;
        mCurrent = next();
    }

    private ReleaseInstaller next() {
        if (mInstallers.size() == 0) {
            return null;
        }
        ReleaseInstaller next = mInstallers.pop();
        AppCenterLog.debug(LOG_TAG, "Trying to install update via " + next.toString() + ".");
        return next;
    }

    @Override
    public synchronized void install(@NonNull Uri localUri) {
        mCancelled = false;
        mLocalUri = localUri;
        if (mCurrent != null) {
            mCurrent.install(localUri);
        }
    }

    @Override
    public synchronized void resume() {
        // Show mandatory dialog if cancelled in background.
        if (mCancelled && mReleaseDetails.isMandatoryUpdate()) {
            Distribute.getInstance().showMandatoryDownloadReadyDialog();
            return;
        }
        if (mCurrent != null) {
            mCurrent.resume();
        }
    }

    @Override
    public synchronized void clear() {
        if (mCurrent != null) {
            mCurrent.clear();
        }
    }

    @Override
    public synchronized void onError(String message) {
        if (isRecoverableError(message)) {
            mCurrent.clear();
            mCurrent = next();
            if (mCurrent != null) {
                mCurrent.install(mLocalUri);
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

    // MIUI fallback
    private static final String RECOVERABLE_ERROR = "INSTALL_FAILED_INTERNAL_ERROR: Permission Denied";

    private static boolean isRecoverableError(String message) {
        return RECOVERABLE_ERROR.equals(message);
    }
}
