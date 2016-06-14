package com.microsoft.android;

import android.app.Application;

import com.microsoft.android.utils.Util;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Avalanche {

    public static final String FEATURE_CRASH = "com.microsoft.android.crash.CrashManager";
    public static final String FEATURE_UPDATE = "com.microsoft.android.update.UpdateManager";
    private static Avalanche sharedInstance;
    private final Set<AvalancheComponent> mComponents;
    private String mAppIdentifier;
    private WeakReference<Application> mApplicationWeakReference;

    protected Avalanche() {
        mComponents = new HashSet<>();
    }

    public static Avalanche getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Avalanche();
        }
        return sharedInstance;
    }

    /**
     * The most convenient way to set up the SDK. It will gather all available features and automatically instantiate and configure them.
     * Instantiation and registration of the features is handled on opinionated basis - the features will register themselves in
     * the most suitable way for them. The app identifier will be read from your manifest.
     *
     * @param application Your application object.
     * @return The Avalanche SDK, fully configured with all features, which are available.
     */
    public static Avalanche configure(Application application) {
        return configure(application, true);
    }

    /**
     * The second-most convenient way to set up the SDK. Offers to option to skip auto configuration of the features.
     * The app identifier will be read from your manifest.
     *
     * @param application   Your application object.
     * @param autoConfigure Whether to auto-configure all available features. If false, only the SDK will be set up.
     * @return The Avalanche SDK, ready to use.
     */
    public static Avalanche configure(Application application, boolean autoConfigure) {
        if (!autoConfigure) {
            return configure(application, new AvalancheComponent[0]);
        }
        String[] allFeatureNames = {FEATURE_CRASH, FEATURE_UPDATE};
        List<Class<? extends AvalancheComponent>> components = new ArrayList<>();
        for (String featureName : allFeatureNames) {
            Class<? extends AvalancheComponent> clazz = getClassForComponent(featureName);
            if (clazz != null) {
                components.add(clazz);
            }
        }
        //noinspection unchecked
        return configure(application, components.toArray(new Class[components.size()]));
    }

    /**
     * Set up the SDK and provide a varargs list of feature classes you would like to have enabled and auto-configured.
     * The app identifier will be read from your manifest.
     *
     * @param application Your application object.
     * @param features    Vararg list of feature classes to auto-configure.
     * @return The Avalanche SDK, configured with your selected features.
     */
    @SafeVarargs
    public static Avalanche configure(Application application, Class<? extends AvalancheComponent>... features) {
        return configure(application, Util.getAppIdentifier(application), features);
    }

    /**
     * Set up the SDK and provide a varargs list of feature classes you would like to have enabled and auto-configured.
     *
     * @param application   Your application object.
     * @param appIdentifier The app identifier to use.
     * @param features      Vararg list of feature classes to auto-configure.
     * @return The Avalanche SDK, configured with your selected features.
     */
    @SafeVarargs
    public static Avalanche configure(Application application, String appIdentifier, Class<? extends AvalancheComponent>... features) {
        List<AvalancheComponent> components = new ArrayList<>();
        if (features != null && features.length > 0) {
            for (Class<? extends AvalancheComponent> featureClass : features) {
                AvalancheComponent component = instantiateComponent(featureClass);
                if (component != null) {
                    components.add(component);
                }
            }
        }

        return configure(application, appIdentifier, components.toArray(new AvalancheComponent[components.size()]));
    }

    /**
     * The most flexible way to set up the SDK. Configure your features first and then pass them in here to enable them in the SDK.
     * The app identifier will be read from your manifest.
     *
     * @param application Your application object.
     * @param features    Vararg list of configured features to enable.
     * @return The Avalanche SDK, configured with the selected feature instances.
     */
    public static Avalanche configure(Application application, AvalancheComponent... features) {
        return configure(application, Util.getAppIdentifier(application), features);
    }

    /**
     * The most flexible way to set up the SDK. Configure your features first and then pass them in here to enable them in the SDK.
     *
     * @param application   Your application object.
     * @param appIdentifier The app identifier to use.
     * @param features      Vararg list of configured features to enable.
     * @return The Avalanche SDK, configured with the selected feature instances.
     */
    public static Avalanche configure(Application application, String appIdentifier, AvalancheComponent... features) {
        Avalanche avalanche = getSharedInstance().initialize(application, appIdentifier);

        if (features != null && features.length > 0) {
            for (AvalancheComponent feature : features) {
                avalanche.addFeature(feature);
            }
        }

        return avalanche;
    }

    /**
     * Checks whether a feature is available at runtime or not.
     *
     * @param componentName The name of the feature you want to check for.
     * @return Whether the feature is available.
     */
    public static boolean isFeatureAvailable(String componentName) {
        return getClassForComponent(componentName) != null;
    }

    private static Class<? extends AvalancheComponent> getClassForComponent(String componentName) {
        try {
            //noinspection unchecked
            return (Class<? extends AvalancheComponent>) Class.forName(componentName);
        } catch (ClassCastException e) {
            // If the class can be resolved but can't be cast to AvalancheComponent, this is no valid component
            return null;
        } catch (ClassNotFoundException e) {
            // If the class can not be resolved, the feature in question is not available.
            return null;
        }
    }

    private static AvalancheComponent instantiateComponent(Class<? extends AvalancheComponent> clazz) {
        //noinspection TryWithIdenticalCatches
        try {
            Method getSharedInstanceMethod = clazz.getMethod("getInstance");
            return (AvalancheComponent) getSharedInstanceMethod.invoke(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Avalanche initialize(Application application, String appIdentifier) {
        mAppIdentifier = appIdentifier;
        mApplicationWeakReference = new WeakReference<>(application);
        mComponents.clear();

        Constants.loadFromContext(application.getApplicationContext());

        return this;
    }

    /**
     * Add and enable a configured feature.
     *
     * @param component
     */
    public void addFeature(AvalancheComponent component) {

        Application application = getApplication();
        if (application != null) {
            application.registerActivityLifecycleCallbacks(component);
            mComponents.add(component);
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
        Class<? extends AvalancheComponent> clazz = getClassForComponent(feature);
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
    public boolean isFeatureEnabled(Class<? extends AvalancheComponent> feature) {
        for (AvalancheComponent component :
                mComponents) {
            if (component.getClass().equals(feature)) {
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
