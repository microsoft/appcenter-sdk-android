package avalanche.base;

import android.app.Application;

import avalanche.base.utils.Util;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AvalancheHub {

    public static final String FEATURE_CRASH = "avalanche.crash.Crashes";

    private static AvalancheHub sharedInstance;
    private final Set<AvalancheFeature> mFeatures;
    private String mAppIdentifier;
    private WeakReference<Application> mApplicationWeakReference;

    protected AvalancheHub() {
        mFeatures = new HashSet<>();
    }

    public static AvalancheHub getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new AvalancheHub();
        }
        return sharedInstance;
    }

    /**
     * The most convenient way to set up the SDK. It will gather all available features and automatically instantiate and use them.
     * Instantiation and registration of the features is handled on opinionated basis - the features will register themselves in
     * the most suitable way for them. The app identifier will be read from your manifest.
     *
     * @param application Your application object.
     * @return The AvalancheHub SDK, fully configured with all features, which are available.
     */
    public static AvalancheHub use(Application application) {
        return use(application, true);
    }

    /**
     * The second-most convenient way to set up the SDK. Offers to option to skip auto configuration of the features.
     * The app identifier will be read from your manifest.
     *
     * @param application   Your application object.
     * @param autoConfigure Whether to auto-use all available features. If false, only the SDK will be set up.
     * @return The AvalancheHub SDK, ready to use.
     */
    public static AvalancheHub use(Application application, boolean autoConfigure) {
        if (!autoConfigure) {
            return use(application, new AvalancheFeature[0]);
        }
        String[] allFeatureNames = {FEATURE_CRASH};
        List<Class<? extends AvalancheFeature>> features = new ArrayList<>();
        for (String featureName : allFeatureNames) {
            Class<? extends AvalancheFeature> clazz = getClassForFeature(featureName);
            if (clazz != null) {
                features.add(clazz);
            }
        }
        //noinspection unchecked
        return use(application, features.toArray(new Class[features.size()]));
    }

    /**
     * Set up the SDK and provide a varargs list of feature classes you would like to have enabled and auto-configured.
     * The app identifier will be read from your manifest.
     *
     * @param application Your application object.
     * @param features    Vararg list of feature classes to auto-use.
     * @return The AvalancheHub SDK, configured with your selected features.
     */
    @SafeVarargs
    public static AvalancheHub use(Application application, Class<? extends AvalancheFeature>... features) {
        return use(application, Util.getAppIdentifier(application), features);
    }

    /**
     * Set up the SDK and provide a varargs list of feature classes you would like to have enabled and auto-configured.
     *
     * @param application   Your application object.
     * @param appIdentifier The app identifier to use.
     * @param features      Vararg list of feature classes to auto-use.
     * @return The AvalancheHub SDK, configured with your selected features.
     */
    @SafeVarargs
    public static AvalancheHub use(Application application, String appIdentifier, Class<? extends AvalancheFeature>... features) {
        List<AvalancheFeature> featureList = new ArrayList<>();
        if (features != null && features.length > 0) {
            for (Class<? extends AvalancheFeature> featureClass : features) {
                AvalancheFeature feature = instantiateFeature(featureClass);
                if (feature != null) {
                    featureList.add(feature);
                }
            }
        }

        return use(application, appIdentifier, featureList.toArray(new AvalancheFeature[featureList.size()]));
    }

    /**
     * The most flexible way to set up the SDK. Configure your features first and then pass them in here to enable them in the SDK.
     * The app identifier will be read from your manifest.
     *
     * @param application Your application object.
     * @param features    Vararg list of configured features to enable.
     * @return The AvalancheHub SDK, configured with the selected feature instances.
     */
    public static AvalancheHub use(Application application, AvalancheFeature... features) {
        return use(application, Util.getAppIdentifier(application), features);
    }

    /**
     * The most flexible way to set up the SDK. Configure your features first and then pass them in here to enable them in the SDK.
     *
     * @param application   Your application object.
     * @param appIdentifier The app identifier to use.
     * @param features      Vararg list of configured features to enable.
     * @return The AvalancheHub SDK, configured with the selected feature instances.
     */
    public static AvalancheHub use(Application application, String appIdentifier, AvalancheFeature... features) {
        AvalancheHub avalancheHub = getSharedInstance().initialize(application, appIdentifier);

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
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private AvalancheHub initialize(Application application, String appIdentifier) {
        mAppIdentifier = appIdentifier;
        mApplicationWeakReference = new WeakReference<>(application);
        mFeatures.clear();

        Constants.loadFromContext(application.getApplicationContext());

        return this;
    }

    /**
     * Add and enable a configured feature.
     *
     * @param feature
     */
    public void addFeature(AvalancheFeature feature) {

        Application application = getApplication();
        if (application != null) {
            application.registerActivityLifecycleCallbacks(feature);
            mFeatures.add(feature);
        }
    }

    /**
     * Get the configured application object.
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
        if (clazz != null) {
            return isFeatureEnabled(clazz);
        }
        return false;
    }

    /**
     * Check whether a feature class is enabled.
     *
     * @param feature    The feature class to check for.
     * @return  Whether the feature is enabled.
     */
    public boolean isFeatureEnabled(Class<? extends AvalancheFeature> feature) {
        for (AvalancheFeature aFeature :
                mFeatures) {
            if (aFeature.getClass().equals(feature)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the configured app identifier.
     * @return The app identifier or null if not set.
     */
    public String getAppIdentifier() {
        return mAppIdentifier;
    }
}
