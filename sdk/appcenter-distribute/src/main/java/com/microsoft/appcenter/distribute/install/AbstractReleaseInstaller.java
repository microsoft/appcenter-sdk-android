package com.microsoft.appcenter.distribute.install;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.Context;
import android.os.Handler;

import com.microsoft.appcenter.utils.AppCenterLog;

public abstract class AbstractReleaseInstaller implements ReleaseInstaller {
    protected final Context mContext;
    private final Handler mInstallerHandler;
    private final Listener mListener;

    protected AbstractReleaseInstaller(Context context, Handler installerHandler, Listener listener) {
        mContext = context;
        mInstallerHandler = installerHandler;
        mListener = listener;
    }

    protected void post(Runnable runnable) {
        mInstallerHandler.post(runnable);
    }

    protected void postDelayed(Runnable runnable, long delayMillis) {
        mInstallerHandler.postDelayed(runnable, delayMillis);
    }

    protected void onError(String message) {
        AppCenterLog.error(LOG_TAG, "Failed to install a new release: " + message);
        mListener.onError(message);
    }

    protected void onCancel() {
        AppCenterLog.debug(LOG_TAG, "Installation cancelled.");
        mListener.onCancel();
    }
}
