/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.text.TextUtils;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HashUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
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
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_SETUP_PATH_FORMAT;

/**
 * Some static util methods to avoid the main file getting too big.
 */
class DistributeUtils {

    /**
     * Scheme used to open the native Android tester app.
     */
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

    /**
     * Update setup using native tester app.
     *
     * @param activity    activity from which to start tester app.
     * @param packageInfo package info.
     */
    static void updateSetupUsingTesterApp(Activity activity, PackageInfo packageInfo) {

        /* Compute hash. */
        String releaseHash = computeReleaseHash(packageInfo);

        /* Generate request identifier. */
        String requestId = UUID.randomUUID().toString();

        /* Build URL. */
        String url = "ms-actesterapp://update-setup";
        url += "?" + PARAMETER_RELEASE_HASH + "=" + releaseHash;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + activity.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId;
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        AppCenterLog.debug(LOG_TAG, "No token, need to open tester app to url=" + url);

        /* Store request id. */
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId);

        /* Open the native tester app */
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    /**
     * Update setup using browser.
     *
     * @param activity    activity from which to start browser.
     * @param installUrl  base install site URL.
     * @param appSecret   application secret.
     * @param packageInfo package info.
     */
    @UiThread
    static void updateSetupUsingBrowser(final Activity activity, String installUrl, String appSecret, PackageInfo packageInfo) {

        /*
         * If network is disconnected, browser will fail so wait.
         * Also we can't just wait for network to be up and launch browser at that time
         * as it's unpredictable and will interrupt the user, so just wait next relaunch.
         */
        if (!NetworkStateHelper.getSharedInstance(activity).isNetworkConnected()) {
            AppCenterLog.info(LOG_TAG, "Postpone enabling in app updates via browser as network is disconnected.");
            Distribute.getInstance().completeWorkflow();
            return;
        }

        /* Compute hash. */
        String releaseHash = computeReleaseHash(packageInfo);

        /* Generate request identifier. */
        String requestId = UUID.randomUUID().toString();

        /* Store request id. */
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId);

        /* Build URL. */
        final StringBuilder urlBuilder = new StringBuilder(installUrl);
        urlBuilder.append(String.format(UPDATE_SETUP_PATH_FORMAT, appSecret));
        urlBuilder.append("?" + PARAMETER_RELEASE_HASH + "=").append(releaseHash);
        urlBuilder.append("&" + PARAMETER_REDIRECT_ID + "=").append(activity.getPackageName());
        urlBuilder.append("&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter");
        urlBuilder.append("&" + PARAMETER_REQUEST_ID + "=").append(requestId);
        urlBuilder.append("&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE);
        urlBuilder.append("&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true");
        AppCenter.getInstallId().thenAccept(new AppCenterConsumer<UUID>() {

            @Override
            public void accept(UUID uuid) {
                urlBuilder.append("&" + PARAMETER_INSTALL_ID + "=").append(uuid.toString());
                String url = urlBuilder.toString();
                AppCenterLog.debug(LOG_TAG, "No token, need to open browser to url=" + url);


                /* Open browser, remember that whatever the outcome to avoid opening it twice. */
                BrowserUtils.openBrowser(url, activity);
            }
        });
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

    static AppCenterFuture<String> getLatestReleaseDetailsUrlAsync(String apiUrl, final String appSecret, final PackageInfo packageInfo, final String distributionGroupId, String updateToken) {
        final DefaultAppCenterFuture<String> future = new DefaultAppCenterFuture<>();
        final String releaseHash = computeReleaseHash(packageInfo);
        final StringBuilder urlBuilder = new StringBuilder(apiUrl);
        if (updateToken == null) {
            AppCenter.getInstallId().thenAccept(new AppCenterConsumer<UUID>() {

                @Override
                public void accept(UUID uuid) {
                    String reportingParameters = getReportingParametersForUpdatedRelease(packageInfo, true, uuid.toString(), null);
                    urlBuilder.append(String.format(GET_LATEST_PUBLIC_RELEASE_PATH_FORMAT, appSecret, distributionGroupId, releaseHash, reportingParameters));
                    future.complete(urlBuilder.toString());
                }
            });
        } else {
            String reportingParameters = getReportingParametersForUpdatedRelease(packageInfo, false, null, distributionGroupId);
            urlBuilder.append(String.format(GET_LATEST_PRIVATE_RELEASE_PATH_FORMAT, appSecret, releaseHash, reportingParameters));
            future.complete(urlBuilder.toString());
        }
        return future;
    }

    /**
     * Get reporting parameters for updated release.
     *
     * @param isPublic            are the parameters for public group or not.
     *                            For public group we report install_id and release_id.
     *                            For private group we report distribution_group_id and release_id.
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
