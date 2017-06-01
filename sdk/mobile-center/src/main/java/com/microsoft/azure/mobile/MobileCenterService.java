package com.microsoft.azure.mobile;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

import java.util.Map;

/**
 * Service specification.
 */
@SuppressWarnings("WeakerAccess")
public interface MobileCenterService extends Application.ActivityLifecycleCallbacks {

    /**
     * Check whether this service is enabled or not.
     *
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    boolean isInstanceEnabled();

    /**
     * Enable or disable this service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    void setInstanceEnabled(boolean enabled);

    /**
     * Gets a name of the service.
     *
     * @return The name of the service.
     */
    String getServiceName();

    /**
     * Factories for logs sent by this service.
     *
     * @return log factories.
     */
    @Nullable
    Map<String, LogFactory> getLogFactories();

    /**
     * Called when a MobileCenter is starting. Storage is not accessible until {@link #onStarted} is called.
     * This is called from the same thread that cause the service to start (determined by user).
     *
     * @param handler background thread handler.
     */
    void onStarting(@NonNull MobileCenterHandler handler);

    /**
     * Called when the service is started (disregarding if enabled or disabled).
     *
     * @param context   application context.
     * @param appSecret application secret.
     * @param channel   channel.
     */
    @WorkerThread
    void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel);
}
