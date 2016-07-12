package avalanche.base;

import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import avalanche.base.utils.AvalancheLog;
import avalanche.base.channel.AvalancheChannel;
import avalanche.base.channel.AvalancheChannelSessionDecorator;
import avalanche.base.channel.DirectAvalancheChannel;
import avalanche.base.ingestion.models.json.DefaultLogSerializer;
import avalanche.base.ingestion.models.json.LogFactory;
import avalanche.base.ingestion.models.json.LogSerializer;
import avalanche.base.utils.StorageHelper;

public final class Avalanche {

    private static Avalanche sharedInstance;
    private final Set<AvalancheFeature> mFeatures;
    private final LogSerializer mLogSerializer = new DefaultLogSerializer();
    private UUID mAppKey;
    private WeakReference<Application> mApplicationWeakReference;
    private boolean mEnabled;
    private AvalancheChannel mChannel;

    protected Avalanche() {
        mEnabled = true;
        mFeatures = new HashSet<>();
    }

    public static Avalanche getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Avalanche();
        }
        return sharedInstance;
    }

    /**
     * Set up the SDK and provide a varargs list of feature classes you would like to have enabled and auto-configured.
     *
     * @param application Your application object.
     * @param appKey      The app key to use (application/environment).
     * @param features    Vararg list of feature classes to auto-use.
     * @return The Avalanche SDK, configured with your selected features.
     */
    @SafeVarargs
    public static Avalanche useFeatures(Application application, String appKey, Class<? extends AvalancheFeature>... features) {
        List<AvalancheFeature> featureList = new ArrayList<>();
        if (features != null && features.length > 0) {
            for (Class<? extends AvalancheFeature> featureClass : features) {
                AvalancheFeature feature = instantiateFeature(featureClass);
                if (feature != null) {
                    featureList.add(feature);
                }
            }
        }

        return useFeatures(application, appKey, featureList.toArray(new AvalancheFeature[featureList.size()]));
    }

    /**
     * The most flexible way to set up the SDK. Configure your features first and then pass them in here to enable them in the SDK.
     *
     * @param application Your application object.
     * @param appKey      The app key to use (application/environment).
     * @param features    Vararg list of configured features to enable.
     * @return The Avalanche SDK, configured with the selected feature instances.
     */
    public static Avalanche useFeatures(Application application, String appKey, AvalancheFeature... features) {
        Avalanche avalancheHub = getSharedInstance().initialize(application, appKey);
        if (features != null && features.length > 0) {
            for (AvalancheFeature feature : features) {
                avalancheHub.addFeature(feature);
            }
        }
        return avalancheHub;
    }

    /**
     * Checks whether a feature is available at runtime or not.
     *
     * @param featureName The name of the feature you want to check for.
     * @return Whether the feature is available.
     */
    public static boolean isFeatureAvailable(String featureName) {
        return getClassForFeature(featureName) != null;
    }

    private static Class<? extends AvalancheFeature> getClassForFeature(String featureName) {
        try {
            //noinspection unchecked
            return (Class<? extends AvalancheFeature>) Class.forName(featureName);
        } catch (ClassCastException e) {
            // If the class can be resolved but can't be cast to AvalancheFeature, this is no valid feature
            return null;
        } catch (ClassNotFoundException e) {
            // If the class can not be resolved, the feature in question is not available.
            return null;
        }
    }

    private static AvalancheFeature instantiateFeature(Class<? extends AvalancheFeature> clazz) {
        //noinspection TryWithIdenticalCatches
        try {
            Method getSharedInstanceMethod = clazz.getMethod("getInstance");
            return (AvalancheFeature) getSharedInstanceMethod.invoke(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("AvalancheFeature subclass must provide public static accessible method getInstance()", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("AvalancheFeature subclass must provide public static accessible method getInstance()", e);
        }
    }

    private Avalanche initialize(Application application, String appKey) {
        if (application == null) {
            throw new IllegalArgumentException("Application must not be null!");
        }

        try {
            mAppKey = UUID.fromString(appKey);
        } catch (NullPointerException e) {
            AvalancheLog.error("App Key must be set for initializing the Avalanche SDK!");
        } catch (IllegalArgumentException e) {
            AvalancheLog.error("App Key is not valid!", e);
        }

        mApplicationWeakReference = new WeakReference<>(application);
        mFeatures.clear();
        if (mChannel == null) { // TODO we must rethink this multiple useFeatures calls
            Context context = application.getApplicationContext();
            Constants.loadFromContext(context);
            StorageHelper.initialize(context);
            AvalancheChannel channel = new DirectAvalancheChannel(context, mAppKey, mLogSerializer); // TODO replace direct by default impl once problems there are fixed
            AvalancheChannelSessionDecorator sessionChannel = new AvalancheChannelSessionDecorator(context, channel);
            application.registerActivityLifecycleCallbacks(sessionChannel);
            mChannel = sessionChannel;
        }
        return this;
    }

    /**
     * Add and enable a configured feature.
     *
     * @param feature feature to add.
     */
    public void addFeature(AvalancheFeature feature) {
        if (feature == null) {
            return;
        }
        Application application = getApplication();
        if (application != null) {

            /*
             * FIXME feature is a list in android so we can have duplicates,
             * anyway we should avoid double initializations when calling useFeatures the second time,
             * we need to make a diff and release removed modules.
             */
            application.unregisterActivityLifecycleCallbacks(feature);
            application.registerActivityLifecycleCallbacks(feature);
            if (feature.getLogFactories() != null) {
                for (Map.Entry<String, LogFactory> logFactory : feature.getLogFactories().entrySet()) {
                    mLogSerializer.addLogFactory(logFactory.getKey(), logFactory.getValue());
                }
            }

            mFeatures.add(feature);
            feature.onChannelReady(mChannel);
        }
    }

    /**
     * Get the configured application object.
     *
     * @return The application instance or null if not set.
     */
    public Application getApplication() {
        return mApplicationWeakReference.get();
    }

    /**
     * Check whether a feature is enabled.
     *
     * @param feature Name of the feature to check.
     * @return Whether the feature is enabled.
     */
    public boolean isFeatureEnabled(String feature) {
        Class<? extends AvalancheFeature> clazz = getClassForFeature(feature);
        return clazz != null && isFeatureEnabled(clazz);
    }

    /**
     * Check whether a feature class is enabled.
     *
     * @param feature The feature class to check for.
     * @return Whether the feature is enabled.
     */
    public boolean isFeatureEnabled(Class<? extends AvalancheFeature> feature) {
        for (AvalancheFeature aFeature : mFeatures) {
            if (aFeature.getClass().equals(feature)) {
                return aFeature.isEnabled();
            }
        }
        return false;
    }

    /**
     * Get the configured app key.
     *
     * @return The app key or null if not set or an invalid value was used when calling {@link #useFeatures(Application, String, AvalancheFeature...)}.
     */
    public String getAppKey() {
        return mAppKey.toString();
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;

        // Set enabled state for every module
        for (AvalancheFeature feature : mFeatures) {
            feature.setEnabled(mEnabled);
        }
    }

    protected Set<AvalancheFeature> getFeatures() {
        return mFeatures;
    }
}
