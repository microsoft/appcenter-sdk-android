package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.Context;
import android.net.Uri;

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
        mInstallers.add(new SessionReleaseInstaller(context, this));
        mInstallers.add(new IntentReleaseInstaller(context, this));
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
    public void install(@NonNull Uri localUri) {
        mCancelled = false;
        mLocalUri = localUri;
        if (mCurrent != null) {
            mCurrent.install(localUri);
        }
    }

    @Override
    public void resume() {
        if (mCancelled) {
            cancel();
            return;
        }
        if (mCurrent != null) {
            mCurrent.resume();
        }
    }

    @Override
    public void clear() {
        if (mCurrent != null) {
            mCurrent.clear();
        }
    }

    @Override
    public void onError(String message) {
        mCurrent.clear();
        mCurrent = next();
        if (mCurrent != null) {
            mCurrent.install(mLocalUri);
        } else {
            Distribute.getInstance().completeWorkflow(mReleaseDetails);
        }
    }

    @Override
    public void onCancel() {
        mCancelled = true;
        cancel();
    }

    private void cancel() {
        if (!mReleaseDetails.isMandatoryUpdate()) {
            Distribute.getInstance().completeWorkflow(mReleaseDetails);
        } else {
            Distribute.getInstance().showMandatoryDownloadReadyDialog();
        }
    }
}
