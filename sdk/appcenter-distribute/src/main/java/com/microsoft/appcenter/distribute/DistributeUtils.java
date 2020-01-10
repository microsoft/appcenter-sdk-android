/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HashUtils;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;

import java.util.UUID;

import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.GET_LATEST_PRIVATE_RELEASE_PATH_FORMAT;
import static com.microsoft.appcenter.distribute.DistributeConstants.GET_LATEST_PUBLIC_RELEASE_PATH_FORMAT;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_INSTALL_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM_VALUE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_SCHEME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_SETUP_PATH_FORMAT;

/**
 * Some static util methods to avoid the main file getting too big.
 */
class DistributeUtils {

    /**
     * Scheme used to open the native Android tester app.
     */
    @VisibleForTesting
    static final String TESTER_APP_PACKAGE_NAME = "com.microsoft.hockeyapp.testerapp";

    /**
     * Get the notification identifier for downloads.
     *
     * @return notification identifier for downloads.
     */
    static int getNotificationId() {
        return Distribute.class.getName().hashCode();
    }

    /**
     * Get download state from storage.
     *
     * @return download state (completed by default).
     */
    static int getStoredDownloadState() {
        return SharedPreferencesManager.getInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_COMPLETED);
    }

    @NonNull
    static String computeReleaseHash(@NonNull PackageInfo packageInfo) {
        return HashUtils.sha256(packageInfo.packageName + ":" + packageInfo.versionName + ":" + DeviceInfoHelper.getVersionCode(packageInfo));
    }

    static boolean shouldUseTesterAppForUpdateSetup(@NonNull Context context) {
        String testerAppUpdateSetupFailedMessage = SharedPreferencesManager.getString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY);
        return isAppCenterTesterAppInstalled(context) && TextUtils.isEmpty(testerAppUpdateSetupFailedMessage) && !context.getPackageName().equals(DistributeUtils.TESTER_APP_PACKAGE_NAME);
    }

    private static boolean isAppCenterTesterAppInstalled(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo(TESTER_APP_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
        return true;
    }

    /**
     * Update setup using native tester app.
     *
     * @param context     context from which to start tester app.
     * @param packageInfo package info.
     */
    static void updateSetupUsingTesterApp(@NonNull final Context context, PackageInfo packageInfo) {
        String url = "ms-actesterapp://update-setup";
        url = getUpdateSetupUrl(url, packageInfo, false);
        AppCenterLog.debug(LOG_TAG, "No token, need to open tester app to url=" + url);

        /* Open the native tester app */
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Update setup using browser.
     *
     * @param context     context from which to start browser.
     * @param installUrl  base install site URL.
     * @param appSecret   application secret.
     * @param packageInfo package info.
     */
    @UiThread
    static void updateSetupUsingBrowser(@NonNull final Context context, String installUrl, String appSecret, PackageInfo packageInfo) {
        String url = installUrl + String.format(UPDATE_SETUP_PATH_FORMAT, appSecret);
        url = getUpdateSetupUrl(url, packageInfo, true);
        AppCenterLog.debug(LOG_TAG, "No token, need to open browser to url=" + url);

        /* Open browser, remember that whatever the outcome to avoid opening it twice. */
        BrowserUtils.openBrowser(url, context);
    }

    /**
     * Get release details from cache if any.
     *
     * @return release details from cache or null.
     */
    static ReleaseDetails loadCachedReleaseDetails() {
        String cachedReleaseDetails = SharedPreferencesManager.getString(PREFERENCE_KEY_RELEASE_DETAILS);
        if (cachedReleaseDetails != null) {
            try {
                return ReleaseDetails.parse(cachedReleaseDetails);
            } catch (JSONException e) {
                AppCenterLog.error(LOG_TAG, "Invalid release details in cache.", e);
                SharedPreferencesManager.remove(PREFERENCE_KEY_RELEASE_DETAILS);
            }
        }
        return null;
    }

    /**
     * Check if latest downloaded release was installed (app was updated).
     *
     * @param packageInfo               current package info.
     * @param lastDownloadedReleaseHash hash of the last downloaded release.
     * @return true if current release was updated.
     */
    static boolean isCurrentReleaseWasUpdated(PackageInfo packageInfo, String lastDownloadedReleaseHash) {
        if (packageInfo == null || TextUtils.isEmpty(lastDownloadedReleaseHash)) {
            return false;
        }
        String currentInstalledReleaseHash = computeReleaseHash(packageInfo);
        return currentInstalledReleaseHash.equals(lastDownloadedReleaseHash);
    }

    /**
     * Check if the fetched release information should be installed.
     *
     * @param packageInfo    current package info.
     * @param releaseDetails latest release on server.
     * @return true if latest release on server should be used.
     */
    static boolean isMoreRecent(PackageInfo packageInfo, ReleaseDetails releaseDetails) {
        boolean moreRecent;
        int versionCode = DeviceInfoHelper.getVersionCode(packageInfo);
        if (releaseDetails.getVersion() == versionCode) {
            moreRecent = !releaseDetails.getReleaseHash().equals(computeReleaseHash(packageInfo));
        } else {
            moreRecent = releaseDetails.getVersion() > versionCode;
        }
        AppCenterLog.debug(LOG_TAG, "Latest release more recent=" + moreRecent);
        return moreRecent;
    }

    /**
     * Get endpoint url to request latest release details.
     *
     * @param apiUrl              current API base URL.
     * @param appSecret           application secret.
     * @param packageInfo         current package info.
     * @param distributionGroupId distribution group id.
     * @param updateToken         token to secure API call.
     * @return future with result being the endpoint url.
     * @see AppCenterFuture
     */
    static String getLatestReleaseDetailsUrl(String apiUrl, final String appSecret, final PackageInfo packageInfo, final String distributionGroupId, String updateToken) {
        final String releaseHash = computeReleaseHash(packageInfo);
        final StringBuilder urlBuilder = new StringBuilder(apiUrl);
        if (updateToken == null) {
            String reportingParameters = getReportingParametersForUpdatedRelease(packageInfo, true, IdHelper.getInstallId().toString(), null);
            urlBuilder.append(String.format(GET_LATEST_PUBLIC_RELEASE_PATH_FORMAT, appSecret, distributionGroupId, releaseHash, reportingParameters));
        } else {
            String reportingParameters = getReportingParametersForUpdatedRelease(packageInfo, false, null, distributionGroupId);
            urlBuilder.append(String.format(GET_LATEST_PRIVATE_RELEASE_PATH_FORMAT, appSecret, releaseHash, reportingParameters));
        }
        return urlBuilder.toString();
    }

    private static String getUpdateSetupUrl(String url, final PackageInfo packageInfo, boolean isBrowser) {

        /* Compute hash. */
        String releaseHash = computeReleaseHash(packageInfo);

        /* Generate request identifier. */
        String requestId = UUID.randomUUID().toString();

        /* Store request id. */
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId);

        /* Build URL. */
        final StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?" + PARAMETER_RELEASE_HASH + "=").append(releaseHash);
        urlBuilder.append("&" + PARAMETER_REDIRECT_ID + "=").append(packageInfo.packageName);
        urlBuilder.append("&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter");
        urlBuilder.append("&" + PARAMETER_REQUEST_ID + "=").append(requestId);
        urlBuilder.append("&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE);
        if (isBrowser) {
            urlBuilder.append("&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true");
            urlBuilder.append("&" + PARAMETER_INSTALL_ID + "=").append(IdHelper.getInstallId().toString());
        }
        return urlBuilder.toString();
    }

    /**
     * Get reporting parameters for updated release.
     *
     * @param packageInfo         current package info.
     * @param isPublic            are the parameters for public group or not.
     *                            For public group we report install_id and release_id.
     *                            For private group we report distribution_group_id and release_id.
     * @param installId           installation id.
     * @param distributionGroupId distribution group id.
     */
    @NonNull
    private static String getReportingParametersForUpdatedRelease(PackageInfo packageInfo, boolean isPublic, String installId, String distributionGroupId) {
        StringBuilder reportingParametersBuilder = new StringBuilder();
        AppCenterLog.debug(LOG_TAG, "Check if we need to report release installation..");
        String lastDownloadedReleaseHash = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
        if (!TextUtils.isEmpty(lastDownloadedReleaseHash)) {
            if (isCurrentReleaseWasUpdated(packageInfo, lastDownloadedReleaseHash)) {
                AppCenterLog.debug(LOG_TAG, "Current release was updated but not reported yet, reporting..");
                if (isPublic) {
                    reportingParametersBuilder.append("&" + PARAMETER_INSTALL_ID + "=").append(installId);
                } else {
                    reportingParametersBuilder.append("&" + PARAMETER_DISTRIBUTION_GROUP_ID + "=").append(distributionGroupId);
                }
                int lastDownloadedReleaseId = SharedPreferencesManager.getInt(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID);
                reportingParametersBuilder.append("&" + PARAMETER_RELEASE_ID + "=").append(lastDownloadedReleaseId);
            } else {
                AppCenterLog.debug(LOG_TAG, "New release was downloaded but not installed yet, skip reporting.");
            }
        } else {
            AppCenterLog.debug(LOG_TAG, "Current release was already reported, skip reporting.");
        }
        return reportingParametersBuilder.toString();
    }
}
