package com.microsoft.appcenter.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.microsoft.appcenter.analytics.Analytics.LOG_TAG;

/**
 * Target for advanced transmission target usage.
 */
public class AnalyticsTransmissionTarget {

    /**
     * Target token for this level.
     */
    private final String mTransmissionTargetToken;

    /**
     * Parent target if any.
     */
    protected final AnalyticsTransmissionTarget mParentTarget;

    /**
     * Children targets for nesting.
     */
    private final Map<String, AnalyticsTransmissionTarget> mChildrenTargets = new HashMap<>();

    /**
     * Property configurator used to override Common Schema Part A properties.
     */
    private PropertyConfigurator mPropertyConfigurator;

    /**
     * Channel used for Property Configurator.
     */
    private Channel mChannel;


    /**
     * The authentication provider to use.
     */
    private static AuthenticationProvider sAuthenticationProvider;

    /**
     * Create a new instance.
     *
     * @param transmissionTargetToken The token for this transmission target.
     * @param parentTarget            Parent transmission target.
     * @param channel                 The channel for this transmission target.
     */
    AnalyticsTransmissionTarget(@NonNull String transmissionTargetToken, final AnalyticsTransmissionTarget parentTarget, Channel channel) {
        mTransmissionTargetToken = transmissionTargetToken;
        mParentTarget = parentTarget;
        mChannel = channel;
        mPropertyConfigurator = new PropertyConfigurator(channel, this);
    }

    /**
     * Add an authentication provider.
     *
     * @param authenticationProvider The authentication provider.
     */
    public static void addAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        sAuthenticationProvider = authenticationProvider;
    }

    /**
     * Track a custom event with name.
     *
     * @param name An event name.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public void trackEvent(String name) {
        trackEvent(name, null);
    }

    /**
     * Track a custom event with name and optional properties.
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    @SuppressWarnings("WeakerAccess")
    public void trackEvent(String name, Map<String, String> properties) {

        /* Merge common properties. More specific target wins conflicts. */
        Map<String, String> mergedProperties = new HashMap<>();
        for (AnalyticsTransmissionTarget target = this; target != null; target = target.mParentTarget) {
            target.getPropertyConfigurator().mergeEventProperties(mergedProperties);
        }

        /* Override with parameter. */
        if (properties != null) {
            mergedProperties.putAll(properties);
        }

        /*
         * If we passed null as parameter and no common properties set,
         * keep null for consistency with Analytics class regarding null vs empty.
         */
        else if (mergedProperties.isEmpty()) {
            mergedProperties = null;
        }

        /* Track event with merged properties. */
        Analytics.trackEvent(name, mergedProperties, this);
    }

    /**
     * Create a new transmission target based on the properties of the current target.
     *
     * @param transmissionTargetToken The transmission target token of the new transmission target.
     * @return The new transmission target.
     */
    public synchronized AnalyticsTransmissionTarget getTransmissionTarget(String transmissionTargetToken) {

        /* Reuse instance if a child with the same token has already been created. */
        AnalyticsTransmissionTarget childTarget = mChildrenTargets.get(transmissionTargetToken);
        if (childTarget == null) {
            childTarget = new AnalyticsTransmissionTarget(transmissionTargetToken, this, mChannel);
            mChildrenTargets.put(transmissionTargetToken, childTarget);
        }
        return childTarget;
    }

    /**
     * Check whether this target is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public AppCenterFuture<Boolean> isEnabledAsync() {
        final DefaultAppCenterFuture<Boolean> future = new DefaultAppCenterFuture<>();
        Analytics.getInstance().postCommand(new Runnable() {

            @Override
            public void run() {
                future.complete(isEnabled());
            }
        }, future, false);
        return future;
    }

    /**
     * Enable or disable this target. The state is applied on all descendant targets.
     * The state is not changed if one ancestor target or Analytics module or AppCenter is disabled.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public AppCenterFuture<Void> setEnabledAsync(final boolean enabled) {
        final DefaultAppCenterFuture<Void> future = new DefaultAppCenterFuture<>();
        Analytics.getInstance().postCommand(new Runnable() {

            @Override
            public void run() {

                /*
                 * Like the relation between AppCenter and Analytics, we cannot change state if one of the parent is disabled.
                 * If this callback is called then it was already checked that AppCenter and Analytics are both enabled.
                 */
                if (areAncestorsEnabled()) {


                    /* Propagate state to this instance then all descendants without a recursive call. */
                    List<AnalyticsTransmissionTarget> descendantTargets = new LinkedList<>();
                    descendantTargets.add(AnalyticsTransmissionTarget.this);
                    while (!descendantTargets.isEmpty()) {
                        ListIterator<AnalyticsTransmissionTarget> descendantIterator = descendantTargets.listIterator();
                        while (descendantIterator.hasNext()) {
                            AnalyticsTransmissionTarget descendantTarget = descendantIterator.next();
                            descendantIterator.remove();
                            StorageHelper.PreferencesStorage.putBoolean(descendantTarget.getEnabledPreferenceKey(), enabled);
                            for (AnalyticsTransmissionTarget childTarget : descendantTarget.mChildrenTargets.values()) {
                                descendantIterator.add(childTarget);
                            }
                        }
                    }
                } else {
                    AppCenterLog.error(LOG_TAG, "One of the parent transmission target is disabled, cannot change state.");
                }
                future.complete(null);
            }
        }, future, null);
        return future;
    }

    /**
     * Getter for transmission target token.
     *
     * @return the transmission target token.
     */
    String getTransmissionTargetToken() {
        return mTransmissionTargetToken;
    }

    /**
     * Getter for property configurator to override Common Schema Part A properties.
     *
     * @return  the Property Configurator
     */
    public PropertyConfigurator getPropertyConfigurator() {
        return mPropertyConfigurator;
    }

    @NonNull
    private String getEnabledPreferenceKey() {
        return Analytics.getInstance().getEnabledPreferenceKeyPrefix() + mTransmissionTargetToken.split("-")[0];
    }

    @WorkerThread
    private boolean isEnabledInStorage() {
        return StorageHelper.PreferencesStorage.getBoolean(getEnabledPreferenceKey(), true);
    }

    @WorkerThread
    private boolean areAncestorsEnabled() {
        for (AnalyticsTransmissionTarget target = mParentTarget; target != null; target = target.mParentTarget) {
            if (!target.isEnabledInStorage()) {
                return false;
            }
        }
        return true;
    }

    @WorkerThread
    boolean isEnabled() {
        return areAncestorsEnabled() && isEnabledInStorage();
    }

    static Channel.Listener getChannelListener() {
        return new AbstractChannelListener() {

            @Override
            public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {
                if (sAuthenticationProvider != null && log instanceof CommonSchemaLog) {
                    CommonSchemaLog csLog = (CommonSchemaLog) log;
                    csLog.getExt().getProtocol().setTicketKeys(Collections.singletonList(sAuthenticationProvider.getTicketKey()));
                }
            }
        };
    }
}
