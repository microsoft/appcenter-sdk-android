/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

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
     * The authentication provider to use.
     */
    @VisibleForTesting
    static AuthenticationProvider sAuthenticationProvider;

    /**
     * Target token for this level.
     */
    private final String mTransmissionTargetToken;

    /**
     * Parent target if any.
     */
    final AnalyticsTransmissionTarget mParentTarget;

    /**
     * Children targets for nesting.
     */
    private final Map<String, AnalyticsTransmissionTarget> mChildrenTargets = new HashMap<>();

    /**
     * Property configurator used to override Common Schema Part A properties.
     */
    private final PropertyConfigurator mPropertyConfigurator;

    /**
     * App context.
     */
    Context mContext;

    /**
     * Channel used for Property Configurator.
     */
    private Channel mChannel;

    /**
     * Create a new instance.
     *
     * @param transmissionTargetToken The token for this transmission target.
     * @param parentTarget            Parent transmission target.
     */
    AnalyticsTransmissionTarget(@NonNull String transmissionTargetToken, final AnalyticsTransmissionTarget parentTarget) {
        mTransmissionTargetToken = transmissionTargetToken;
        mParentTarget = parentTarget;
        mPropertyConfigurator = new PropertyConfigurator(this);
    }

    @WorkerThread
    void initInBackground(Context context, Channel channel) {
        mContext = context;
        mChannel = channel;
        channel.addListener(mPropertyConfigurator);
    }

    /**
     * Add an authentication provider to associate logs with user identifiers.
     * Events tracked previous to calling this method are not impacted by this call.
     * If the callback from the authentication provider takes a long time to process,
     * events might be tracked anonymously.
     * <p>
     * If you want to wait for authentication callback to complete before processing logs,
     * you can pause and resume Analytics or a specific transmission target.
     *
     * @param authenticationProvider The authentication provider.
     */
    public static synchronized void addAuthenticationProvider(final AuthenticationProvider authenticationProvider) {

        /* Validate input. */
        if (authenticationProvider == null) {
            AppCenterLog.error(LOG_TAG, "Authentication provider may not be null.");
            return;
        }
        if (authenticationProvider.getType() == null) {
            AppCenterLog.error(LOG_TAG, "Authentication provider type may not be null.");
            return;
        }
        if (authenticationProvider.getTicketKey() == null) {
            AppCenterLog.error(LOG_TAG, "Authentication ticket key may not be null.");
            return;
        }
        if (authenticationProvider.getTokenProvider() == null) {
            AppCenterLog.error(LOG_TAG, "Authentication token provider may not be null.");
            return;
        }

        /* Update current provider. */
        if (AppCenter.isConfigured()) {
            Analytics.getInstance().postCommandEvenIfDisabled(new Runnable() {

                @Override
                public void run() {
                    updateProvider(authenticationProvider);
                }
            });
        } else {
            updateProvider(authenticationProvider);
        }
    }

    /**
     * Update authentication provider.
     *
     * @param authenticationProvider the new authentication provider.
     */
    private static void updateProvider(AuthenticationProvider authenticationProvider) {

        /* Update reference. */
        sAuthenticationProvider = authenticationProvider;

        /* Request token now. */
        authenticationProvider.acquireTokenAsync();
    }

    /**
     * Track a custom event with name.
     * <p>
     * The name cannot be null and needs to match the
     * <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.
     *
     * @param name An event name.
     */
    public void trackEvent(String name) {
        trackEvent(name, (EventProperties) null, Flags.DEFAULTS);
    }

    /**
     * Track a custom event with name and optional string properties.
     * <p>
     * The following rules apply:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The property names or values cannot be null.</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    public void trackEvent(String name, Map<String, String> properties) {
        trackEvent(name, properties, Flags.DEFAULTS);
    }

    /**
     * Track a custom event with name and optional string properties.
     * <p>
     * The following rules apply:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The property names or values cannot be null.</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     * @param flags      Optional flags. Events tracked with the {@link Flags#CRITICAL}
     *                   flag will take precedence over all other events in storage.
     *                   An event tracked with this option will only be dropped
     *                   if storage must make room for a newer event that is also marked with the
     *                   {@link Flags#CRITICAL} flag.
     */
    public void trackEvent(String name, Map<String, String> properties, int flags) {
        EventProperties eventProperties = null;
        if (properties != null) {
            eventProperties = new EventProperties();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                eventProperties.set(entry.getKey(), entry.getValue());
            }
        }
        trackEvent(name, eventProperties, flags);
    }

    /**
     * Track a custom event with name and optional string properties.
     * <p>
     * The following rules apply:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The property names or values cannot be null.</li>
     * <li>Double values must be finite (NaN or Infinite values are discarded).</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    public void trackEvent(String name, EventProperties properties) {
        trackEvent(name, properties, Flags.DEFAULTS);
    }

    /**
     * Track a custom event with name and optional string properties.
     * <p>
     * The following rules apply:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The property names or values cannot be null.</li>
     * <li>Double values must be finite (NaN or Infinite values are discarded).</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     * @param flags      Optional flags. Events tracked with the {@link Flags#CRITICAL}
     *                   flag will take precedence over all other events in storage.
     *                   An event tracked with this option will only be dropped
     *                   if storage must make room for a newer event that is also marked with the
     *                   {@link Flags#CRITICAL} flag.
     */
    public void trackEvent(String name, EventProperties properties, int flags) {

        /* Merge common properties. More specific target wins conflicts. */
        EventProperties mergedProperties = new EventProperties();
        for (AnalyticsTransmissionTarget target = this; target != null; target = target.mParentTarget) {
            target.getPropertyConfigurator().mergeEventProperties(mergedProperties);
        }

        /* Override with parameter. */
        if (properties != null) {
            mergedProperties.getProperties().putAll(properties.getProperties());
        }

        /*
         * If we passed null as parameter and no common properties set,
         * keep null for consistency with Analytics class regarding null vs empty.
         */
        else if (mergedProperties.getProperties().isEmpty()) {
            mergedProperties = null;
        }

        /* Track event with merged properties. */
        Analytics.trackEvent(name, mergedProperties, this, flags);
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
            childTarget = new AnalyticsTransmissionTarget(transmissionTargetToken, this);
            mChildrenTargets.put(transmissionTargetToken, childTarget);
            final AnalyticsTransmissionTarget finalChildTarget = childTarget;
            Analytics.getInstance().postCommandEvenIfDisabled(new Runnable() {

                @Override
                public void run() {
                    finalChildTarget.initInBackground(mContext, mChannel);
                }
            });
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
                            SharedPreferencesManager.putBoolean(descendantTarget.getEnabledPreferenceKey(), enabled);
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
     * Pauses log transmission for this target.
     * This does not pause child targets.
     */
    public void pause() {
        Analytics.getInstance().post(new Runnable() {

            @Override
            public void run() {
                mChannel.pauseGroup(Analytics.ANALYTICS_GROUP, mTransmissionTargetToken);
                mChannel.pauseGroup(Analytics.ANALYTICS_CRITICAL_GROUP, mTransmissionTargetToken);
            }
        });
    }

    /**
     * Resumes log transmission for this target.
     * This does not resume child targets.
     */
    public void resume() {
        Analytics.getInstance().post(new Runnable() {

            @Override
            public void run() {
                mChannel.resumeGroup(Analytics.ANALYTICS_GROUP, mTransmissionTargetToken);
                mChannel.resumeGroup(Analytics.ANALYTICS_CRITICAL_GROUP, mTransmissionTargetToken);
            }
        });
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
     * Init channel listener to add tickets to logs.
     */
    static Channel.Listener getChannelListener() {
        return new AbstractChannelListener() {

            @Override
            public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {
                addTicketToLog(log);
            }
        };
    }

    /**
     * Add ticket to common schema logs.
     */
    private static void addTicketToLog(@NonNull Log log) {

        /* Decorate only common schema logs when an authentication provider was registered. */
        if (sAuthenticationProvider != null && log instanceof CommonSchemaLog) {

            /* Add ticket reference to log. */
            CommonSchemaLog csLog = (CommonSchemaLog) log;
            String ticketKey = sAuthenticationProvider.getTicketKeyHash();
            csLog.getExt().getProtocol().setTicketKeys(Collections.singletonList(ticketKey));

            /*
             * Check if we should try to refresh token if soon expired.
             * Known corner case: if already expired and refresh takes longer than batching log time,
             * then next logs will be anonymous until token refreshed.
             */
            sAuthenticationProvider.checkTokenExpiry();
        }
    }

    @NonNull
    private String getEnabledPreferenceKey() {
        return Analytics.getInstance().getEnabledPreferenceKeyPrefix() + PartAUtils.getTargetKey(mTransmissionTargetToken);
    }

    @WorkerThread
    private boolean isEnabledInStorage() {
        return SharedPreferencesManager.getBoolean(getEnabledPreferenceKey(), true);
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

    /**
     * Getter for property configurator to override Common Schema Part A properties.
     *
     * @return the Property Configurator
     */
    public PropertyConfigurator getPropertyConfigurator() {
        return mPropertyConfigurator;
    }
}
