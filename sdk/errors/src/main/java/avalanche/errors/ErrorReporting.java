package avalanche.errors;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import avalanche.core.AbstractAvalancheFeature;
import avalanche.core.Avalanche;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.Util;


public class ErrorReporting extends AbstractAvalancheFeature {

    private static ErrorReporting sInstance = null;

    private final Map<String, LogFactory> mFactories;

    private ErrorReportingListener mListener;

    private WeakReference<Context> mContextWeakReference;
    private long mInitializeTimestamp;
    private Thread.UncaughtExceptionHandler mPreviousUncaughtExceptionHandler;

    private ErrorReporting() {
        mFactories = new HashMap<>();
        // Add crash log factory here
    }

    @NonNull
    public static ErrorReporting getInstance() {
        if (sInstance == null) {
            sInstance = new ErrorReporting();
        }
        return sInstance;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        initialize();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        super.onActivityResumed(activity);
        if (mContextWeakReference == null && Util.isMainActivity(activity)) {
            // Opinionated approach -> per default we will want to activate the crash reporting with the very first of your activities.
            register(activity);
        }
    }

    public static void register(@NonNull Context context) {
        register(context, null);
    }

    public static void register(@NonNull Context context, @Nullable ErrorReportingListener listener) {
        ErrorReporting errorReporting = getInstance();
        errorReporting.mContextWeakReference = new WeakReference<Context>(context);
        errorReporting.initialize();
    }

    private void initialize() {
        boolean enabled = isEnabled();
        mInitializeTimestamp = enabled ? System.currentTimeMillis() : -1;

        registerExceptionHandler();
    }

    private void queuePendingCrashes() {
        // Add the pending crashes to the corresponding log
    }

}
