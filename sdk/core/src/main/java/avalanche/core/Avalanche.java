package avalanche.core;

import android.app.Application;
import android.support.annotation.VisibleForTesting;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.channel.AvalancheChannelSessionDecorator;
import avalanche.core.channel.DefaultAvalancheChannel;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.IdHelper;
import avalanche.core.utils.StorageHelper;

public final class Avalanche { 

    /**
     * Share instance.
     */
    private static Avalanche sInstance;

    /**
     * Application context.
     */
    private Application mApplication;

    /**
     * Configured features.
     */
    private Set<AvalancheFeature> mFeatures;

    /**
     * Enabled state.
     */
    private boolean mEnabled = true;

    /**
     * Log serializer.
     */
    private LogSerializer mLogSerializer;

    /**
     * Channel.
     */
    private AvalancheChannelSessionDecorator mChannel;

    @VisibleForTesting
    static Avalanche getInstance() {
        if (sInstance == null)
            sInstance = new Avalanche();
        return sInstance;
    }

    /**
     * Set up the SDK and provide a varargs list of feature classes you would like to have enabled and auto-configured.
     *
     * @param application Your application object.
     * @param appKey      The app key to use (application/environment).
     * @param features    Vararg list of feature classes to auto-use.
     */
    @SafeVarargs
    public static void useFeatures(Application application, String appKey, Class<? extends AvalancheFeature>... features) {
        List<AvalancheFeature> featureList = new ArrayList<>();
        for (Class<? extends AvalancheFeature> featureClass : features)
            if (featureClass != null) {
                AvalancheFeature feature = instantiateFeature(featureClass);
                if (feature != null)
                    featureList.add(feature);
            }
        useFeatures(application, appKey, featureList.toArray(new AvalancheFeature[featureList.size()]));
    }

    /**
     * The most flexible way to set up the SDK. Configure your features first and then pass them in here to enable them in the SDK.
     *
     * @param application Your application object.
     * @param appKey      The app key to use (application/environment).
     * @param features    Vararg list of configured features to enable.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void useFeatures(Application application, String appKey, AvalancheFeature... features) {
        Avalanche instance = getInstance();
        synchronized (instance) {
            boolean initializedSuccessfully = instance.initialize(application, appKey);
            if (initializedSuccessfully)
                for (AvalancheFeature feature : features)
                    instance.addFeature(feature);
        }
    }

    private static AvalancheFeature instantiateFeature(Class<? extends AvalancheFeature> type) {
        try {
            Method getInstance = type.getMethod("getInstance");
            return (AvalancheFeature) getInstance.invoke(null);
        } catch (Exception e) {
            AvalancheLog.error("Failed to instantiate feature '" + type.getName() + "'", e);
            return null;
        }
    }

    public static boolean isEnabled() {
        return getInstance().mIsEnabled();
    }

    public static void setEnabled(boolean enabled) {
        getInstance().mSetEnabled(enabled);
    }

    /**
     * Get unique installation identifier.
     *
     * @return unique install identifier.
     */
    public static UUID getInstallId() {
        return IdHelper.getInstallId();
    }

    @VisibleForTesting
    static void unsetInstance() {
        sInstance = null;
    }

    private synchronized boolean mIsEnabled() {
        return mEnabled;
    }

    private synchronized void mSetEnabled(boolean enabled) {

        /* Update channel state. */
        mChannel.setEnabled(enabled);

        /* Un-subscribe app callbacks if we were enabled and now disabled. */
        boolean switchToDisabled = mEnabled && !enabled;
        boolean switchToEnabled = !mEnabled && enabled;
        if (switchToDisabled) {
            mApplication.unregisterActivityLifecycleCallbacks(mChannel);
            AvalancheLog.info("Avalanche disabled");
        } else if (switchToEnabled) {
            mApplication.registerActivityLifecycleCallbacks(mChannel);
            AvalancheLog.info("Avalanche enabled");
        }

        /* Apply change to features. */
        for (AvalancheFeature feature : mFeatures) {

            /* Add or remove callbacks depending on state change. */
            if (switchToDisabled)
                mApplication.unregisterActivityLifecycleCallbacks(feature);
            else if (switchToEnabled)
                mApplication.registerActivityLifecycleCallbacks(feature);

            /* Forward status change. */
            if (feature.isEnabled() != enabled)
                feature.setEnabled(enabled);
        }

        /* Update state. */
        mEnabled = enabled;
    }

    private boolean initialize(Application application, String appKey) {

        /* Parse and store parameters. */
        if (mApplication != null) {
            AvalancheLog.warn("Avalanche may only be init once");
            return false;
        }
        if (application == null) {
            AvalancheLog.error("application may not be null");
            return false;
        }
        if (appKey == null) {
            AvalancheLog.error("appKey may not be null");
            return false;
        }
        UUID appKeyUUID;
        try {
            appKeyUUID = UUID.fromString(appKey);
        } catch (IllegalArgumentException e) {
            AvalancheLog.error("appKey is invalid", e);
            return false;
        }
        mApplication = application;

        /* If parameters are valid, init context related resources. */
        Constants.loadFromContext(application);
        StorageHelper.initialize(application);
        mFeatures = new HashSet<>();

        /* Init channel. */
        mLogSerializer = new DefaultLogSerializer();
        AvalancheChannel channel = new DefaultAvalancheChannel(application, appKeyUUID, mLogSerializer);
        AvalancheChannelSessionDecorator sessionChannel = new AvalancheChannelSessionDecorator(application, channel);
        application.registerActivityLifecycleCallbacks(sessionChannel);
        mChannel = sessionChannel;
        return true;
    }

    /**
     * Add and enable a configured feature.
     *
     * @param feature feature to add.
     */
    private void addFeature(AvalancheFeature feature) {
        if (feature == null)
            return;
        Map<String, LogFactory> logFactories = feature.getLogFactories();
        if (logFactories != null) {
            for (Map.Entry<String, LogFactory> logFactory : logFactories.entrySet())
                mLogSerializer.addLogFactory(logFactory.getKey(), logFactory.getValue());
        }
        mFeatures.add(feature);
        feature.onChannelReady(mChannel);
        mApplication.registerActivityLifecycleCallbacks(feature);
    }

    @VisibleForTesting
    Set<AvalancheFeature> getFeatures() {
        return mFeatures;
    }

    @VisibleForTesting
    Application getApplication() {
        return mApplication;
    }

    @VisibleForTesting
    void setChannel(AvalancheChannelSessionDecorator channel) {
        mChannel = channel;
    }
}
