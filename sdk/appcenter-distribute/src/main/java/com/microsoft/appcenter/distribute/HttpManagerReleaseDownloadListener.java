/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.File;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

public class HttpManagerReleaseDownloadListener extends ManagerReleaseDownloadListener {

    public HttpManagerReleaseDownloadListener(@NonNull Context context) {
        super(context);
    }

    /**
     * Get the intent used to open installation U.I.
     *
     * @param fileUri downloaded file URI from the download manager.
     * @return intent to open installation U.I.
     */
    @NonNull
    @Override
    protected Intent getInstallIntent(Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setDataAndType(fileUri,
                    "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    public void onComplete(@NonNull String localUri, @NonNull ReleaseDetails releaseDetails) {
        AppCenterLog.debug(LOG_TAG, "Download was successful uri=" + localUri);
        Uri uri = Uri.fromFile(new File(localUri));
        Intent intent = getInstallIntent(uri);
        boolean installerFound = intent.resolveActivity(mContext.getPackageManager()) != null;
        if (!installerFound) {
            AppCenterLog.error(LOG_TAG, "Installer not found");
//            distribute.completeWorkflow(mReleaseDetails);
            return;
        }

        // TODO Check if a should install now.

        AppCenterLog.info(LOG_TAG, "Show install UI now intentUri=" + intent.getData());
        mContext.startActivity(intent);
        if (releaseDetails.isMandatoryUpdate()) {
            Distribute.getInstance().setInstalling(releaseDetails);
        }
        storeReleaseDetails(releaseDetails);
    }
}
