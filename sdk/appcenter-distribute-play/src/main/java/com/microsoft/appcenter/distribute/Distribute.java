/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

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
        AppCenterLog.debug(LOG_TAG, "Called method 'isEnabled'");
        DefaultAppCenterFuture tt = new DefaultAppCenterFuture<>();
        tt.complete(true);
        return tt;
    }

    /**
     * Enable or disable Distribute service.
     * <p>
     * The state is persisted in the device's storage across application launches.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        AppCenterLog.debug(LOG_TAG, "Called method 'setEnabled'");
        DefaultAppCenterFuture tt = new DefaultAppCenterFuture<>();
        tt.complete(true);
        return tt;
    }

    /**
     * Implements {@link #setInstallUrl(String)}.
     */
    public static void setInstallUrl(String installUrl) {
        AppCenterLog.debug(LOG_TAG, "Called method 'setInstallUrl'");
    }

    /**
     * Implements {@link #setApiUrl(String)}}.
     */
    public static void setApiUrl(String apiUrl) {
        AppCenterLog.debug(LOG_TAG, "Called method 'setApiUrl'");
    }

    /**
     * Get the current update track (public vs private).
     */
    public static int getUpdateTrack() {
        AppCenterLog.debug(LOG_TAG, "Called method 'getUpdateTrack'");
        return UpdateTrack.PUBLIC;
    }

    /**
     * Set the update track (public vs private).
     *
     * @param updateTrack update track.
     */
    public static void setUpdateTrack(@UpdateTrack int updateTrack) {
        AppCenterLog.debug(LOG_TAG, "Called method 'setUpdateTrack'");
    }

    /**
     * Sets a distribute listener.
     *
     * @param listener The custom distribute listener.
     */
    public static void setListener(DistributeListener listener) {
        AppCenterLog.debug(LOG_TAG, "Called method 'setListener'");
    }

    /**
     * Implements {@link #setEnabledForDebuggableBuild(boolean)}.
     */
    public static void setEnabledForDebuggableBuild(boolean enabled) {
        AppCenterLog.debug(LOG_TAG, "Called method 'setEnabledForDebuggableBuild'");
    }

    /**
     * If update dialog is customized by returning <code>true</code> in  {@link DistributeListener#onReleaseAvailable(Activity, ReleaseDetails)},
     * You need to tell the distribute SDK using this function what is the user action.
     *
     * @param updateAction one of {@link UpdateAction} actions.
     *                     For mandatory updates, only {@link UpdateAction#UPDATE} is allowed.
     */
    public static void notifyUpdateAction(@UpdateAction int updateAction) {
        AppCenterLog.debug(LOG_TAG, "Called method 'notifyUpdateAction'");
    }

    /**
     * Implements {@link #checkForUpdate()}.
     */
    public static void checkForUpdate() {
        AppCenterLog.debug(LOG_TAG, "Called method 'checkForUpdate'");
    }

    /**
     * Disable automatic check for update before the service starts.
     */
    public static void disableAutomaticCheckForUpdate() {
        AppCenterLog.debug(LOG_TAG, "Called method 'disableAutomaticCheckForUpdate'");
    }

    @Override
    protected String getGroupName() {
        AppCenterLog.debug(LOG_TAG, "Called method 'getGroupName'");
        return DISTRIBUTE_GROUP;
    }

    @Override
    public String getServiceName() {
        AppCenterLog.debug(LOG_TAG, "Called method 'getServiceName'");
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        AppCenterLog.debug(LOG_TAG, "Called method 'getLoggerTag'");
        return LOG_TAG;
    }

    @Override
    protected int getTriggerCount() {
        AppCenterLog.debug(LOG_TAG, "Called method 'getTriggerCount'");
        return 1;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        AppCenterLog.debug(LOG_TAG, "Called method 'getLogFactories'");
        return new HashMap<>();
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        AppCenterLog.debug(LOG_TAG, "Called method 'onStarted'");
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        AppCenterLog.debug(LOG_TAG, "Called method 'onActivityResumed'");
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        AppCenterLog.debug(LOG_TAG, "Called method 'onActivityPaused'");
    }

    @Override
    public void onApplicationEnterForeground() {
        AppCenterLog.debug(LOG_TAG, "Called method 'onApplicationEnterForeground'");
    }

    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        AppCenterLog.debug(LOG_TAG, "Called method 'applyEnabledState'");
    }
}
