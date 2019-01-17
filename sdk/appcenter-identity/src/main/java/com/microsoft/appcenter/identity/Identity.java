package com.microsoft.appcenter.identity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

/**
 * identity service.
 */
public class Identity extends AbstractAppCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Identity";

    /**
     * TAG used in logging for Identity.
     */
    private static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the identity group.
     */
    static final String IDENTITY_GROUP = "group_identity";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Identity sInstance;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Identity getInstance() {
        if (sInstance == null) {
            sInstance = new Identity();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check whether Identity service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings("unused")
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Identity service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings("unused")
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
            //TODO: add implementation.
        } else {
             //TODO: add implementation.
        }
    }

    @Override
    protected String getGroupName() {
        return IDENTITY_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }
}
