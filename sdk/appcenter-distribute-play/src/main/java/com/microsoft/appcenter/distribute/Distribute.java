/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.SERVICE_NAME;

/**
 * Distribute service.
 */
public class Distribute extends AbstractAppCenterService {

    private static Distribute sInstance;

    private static final String DISTRIBUTE_GROUP = "group_distribute";

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    public static synchronized Distribute getInstance() {
        if (sInstance == null) {
            sInstance = new Distribute();
        }
        return sInstance;
    }

    /**
     * Check whether Distribute service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        DefaultAppCenterFuture appCenterFuture = new DefaultAppCenterFuture<>();
        appCenterFuture.complete(true);
        return appCenterFuture;
    }

    /**
     * Enable or disable Distribute service.
     * 
     * The state is persisted in the device's storage across application launches.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        DefaultAppCenterFuture appCenterFuture = new DefaultAppCenterFuture<>();
        appCenterFuture.complete(true);
        return appCenterFuture;
    }

    /**
     * Change the base URL opened in the browser to get update token from user login information.
     *
     * @param installUrl install base URL.
     */
    public static void setInstallUrl(String installUrl) {
    }

    /**
     * Change the base URL used to make API calls.
     *
     * @param apiUrl API base URL.
     */
    public static void setApiUrl(String apiUrl) {
    }

    /**
     * Get the current update track (public vs private).
     */
    public static int getUpdateTrack() {
        return UpdateTrack.PUBLIC;
    }

    /**
     * Set the update track (public vs private).
     *
     * @param updateTrack update track.
     */
    public static void setUpdateTrack(@UpdateTrack int updateTrack) {
    }

    /**
     * Sets a distribute listener.
     *
     * @param listener The custom distribute listener.
     */
    public static void setListener(DistributeListener listener) {
    }

    /**
     * Set whether the distribute service can be used within a debuggable build.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    public static void setEnabledForDebuggableBuild(boolean enabled) {
    }

    /**
     * If update dialog is customized by returning <code>true</code> in  {@link DistributeListener#onReleaseAvailable(Activity, ReleaseDetails)},
     * You need to tell the distribute SDK using this function what is the user action.
     *
     * @param updateAction one of {@link UpdateAction} actions.
     *                     For mandatory updates, only {@link UpdateAction#UPDATE} is allowed.
     */
    public static void notifyUpdateAction(@UpdateAction int updateAction) {
    }

    /**
     * Implements {@link #notifyUpdateAction(int)}.
     */
    synchronized void handleUpdateAction(final int updateAction) {
    }

    /**
     * Trigger a check for update.
     * If the application is in background, it will delay the check for update until the application is in foreground.
     * This call has no effect if there is already an ongoing check.
     */
    public static void checkForUpdate() {
    }

    /**
     * Disable automatic check for update before the service starts.
     */
    public static void disableAutomaticCheckForUpdate() {
    }

    @Override
    protected String getGroupName() {
        return DISTRIBUTE_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    protected int getTriggerCount() {
        return 1;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return new HashMap<>();
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
    }

    @Override
    public void onApplicationEnterForeground() {
    }

    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
    }
}
