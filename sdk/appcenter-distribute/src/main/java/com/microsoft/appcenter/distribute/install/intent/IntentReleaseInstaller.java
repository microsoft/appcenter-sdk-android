/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.intent;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

import com.microsoft.appcenter.distribute.install.AbstractReleaseInstaller;
import com.microsoft.appcenter.distribute.install.ReleaseInstallerActivity;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

/**
 * Installer based on {@link Intent#ACTION_INSTALL_PACKAGE}.
 */
public class IntentReleaseInstaller extends AbstractReleaseInstaller {

    public IntentReleaseInstaller(Context context, Handler installerHandler, Listener listener) {
        super(context, installerHandler, listener);
    }

    @AnyThread
    @Override
    public void install(@NonNull Uri localUri) {
        final Intent installIntent = getInstallIntent(localUri);
        if (installIntent.resolveActivity(mContext.getPackageManager()) == null) {
            onError("Cannot resolve install intent for " + localUri);
            return;
        }

        /* Use proxy activity to handle activity result. */
        AppCenterFuture<ReleaseInstallerActivity.Result> confirmFuture = ReleaseInstallerActivity.startActivityForResult(mContext, installIntent);
        if (confirmFuture == null) {

            /* Another installing activity already in progress. Precaution for unexpected case. */
            return;
        }
        confirmFuture.thenAccept(new AppCenterConsumer<ReleaseInstallerActivity.Result>() {

            @Override
            public void accept(ReleaseInstallerActivity.Result result) {
                if (result.code == RESULT_FIRST_USER) {
                    onError("Install failed");
                } else if (result.code == RESULT_CANCELED) {
                    onCancel();
                }
            }
        });
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
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        return intent;
    }
}
