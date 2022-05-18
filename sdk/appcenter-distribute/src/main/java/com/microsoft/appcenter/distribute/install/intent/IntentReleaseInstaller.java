package com.microsoft.appcenter.distribute.install.intent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.install.AbstractReleaseInstaller;

public class IntentReleaseInstaller extends AbstractReleaseInstaller {

    public IntentReleaseInstaller(Context context, Listener listener) {
        super(context, listener);
    }

    @WorkerThread
    @Override
    public void install(@NonNull Uri localUri) {
        final Intent intent = getInstallIntent(localUri);
        if (intent.resolveActivity(mContext.getPackageManager()) == null) {
            mListener.onError("Cannot resolve install intent for " + localUri);
            return;
        }
        mContext.startActivity(intent);
    }

    @Override
    public void resume() {
        // If we're still here then it was cancelled.
        mListener.onCancel();
    }

    @Override
    public void clear() {
        // Nothing to clear.
    }

    @NonNull
    @Override
    public String toString() {
        return "ACTION_INSTALL_PACKAGE";
    }

    /**
     * Get the intent used to open installation UI.
     *
     * @param fileUri downloaded file URI from the download manager.
     * @return intent to open installation UI.
     */
    @NonNull
    protected static Intent getInstallIntent(Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
}
