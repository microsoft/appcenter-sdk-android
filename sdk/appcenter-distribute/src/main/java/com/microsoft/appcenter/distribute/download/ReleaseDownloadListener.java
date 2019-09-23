/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.download.DownloadUtils.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.download.DownloadUtils.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.download.DownloadUtils.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;

public class ReleaseDownloadListener implements ReleaseDownloader.Listener {

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    public ReleaseDownloadListener(@NonNull Context context) {
        mContext = context;
    }

    private static Uri getFileUriOnOldDevices(String localUrl) {
        return Uri.parse("file://" + localUrl);
    }

    static void storeReleaseDetails(@NonNull ReleaseDetails releaseDetails) {
        String groupId = releaseDetails.getDistributionGroupId();
        String releaseHash = releaseDetails.getReleaseHash();
        int releaseId = releaseDetails.getId();
        AppCenterLog.debug(LOG_TAG, "Stored release details: group id=" + groupId + " release hash=" + releaseHash + " release id=" + releaseId);
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID, groupId);
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH, releaseHash);
        SharedPreferencesManager.putInt(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID, releaseId);
    }

    /**
     * Get the intent used to open installation U.I.
     *
     * @param fileUri downloaded file URI from the download manager.
     * @return intent to open installation U.I.
     */
    @NonNull
    static Intent getInstallIntent(Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    public void onProgress(long downloadedBytes, long totalBytes) {
        AppCenterLog.verbose(LOG_TAG, "downloadedBytes=" + downloadedBytes + " totalBytes=" + totalBytes);
    }

    @Override
    public void onComplete(@NonNull String localUri, @NonNull ReleaseDetails releaseDetails) {
        AppCenterLog.debug(LOG_TAG, "Download was successful uri=" + localUri);
        Intent intent = getInstallIntent(Uri.parse(localUri));
        boolean installerFound = intent.resolveActivity(mContext.getPackageManager()) != null;
//      if (!installerFound) {
//          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//              intent = DownloadUtils.getInstallIntent(getFileUriOnOldDevices(cursor));
//              installerFound = intent.resolveActivity(mContext.getPackageManager()) != null;
//          }
//      } else {
//          installerFound = true;
//      }
        if (!installerFound) {
            AppCenterLog.error(LOG_TAG, "Installer not found");
        }

        AppCenterLog.info(LOG_TAG, "Show install UI now intentUri=" + intent.getData());
        mContext.startActivity(intent);
        if (releaseDetails.isMandatoryUpdate()) {
            Distribute.getInstance().setInstalling(releaseDetails);
        }
        storeReleaseDetails(releaseDetails);
    }

    @Override
    public void onError(String errorMessage) {

    }
}
