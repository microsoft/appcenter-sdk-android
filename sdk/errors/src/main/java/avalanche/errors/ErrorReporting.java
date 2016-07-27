package avalanche.errors;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import avalanche.core.AbstractAvalancheFeature;
import avalanche.core.ingestion.models.Device;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.Util;


public class ErrorReporting extends AbstractAvalancheFeature {

    public static final String ERROR_GROUP = "group_error";

    private static ErrorReporting sInstance = null;

    private final Map<String, LogFactory> mFactories;

    private ErrorReportingListener mListener;

    private WeakReference<Context> mContextWeakReference;
    private long mInitializeTimestamp;
    private UncaughtExceptionHandler mUncaughtExceptionHandler;
    private Device mDevice;

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

    public static void register(@NonNull Context context) {
        register(context, null);
    }

    public static void register(@NonNull Context context, @Nullable ErrorReportingListener listener) {
        ErrorReporting errorReporting = getInstance();
        errorReporting.mContextWeakReference = new WeakReference<>(context);
        try {
            errorReporting.mDevice = DeviceInfoHelper.getDeviceInfo(context);
        } catch (DeviceInfoHelper.DeviceInfoException e) {
            e.printStackTrace();
        }
        errorReporting.initialize();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        super.onActivityResumed(activity);
        if (mContextWeakReference == null && Util.isMainActivity(activity)) {
            // Opinionated approach -> per default we will want to activate the crash reporting with the very first of your activities.
            register(activity);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        initialize();
    }

    @Override
    protected String getGroupName() {
        return ERROR_GROUP;
    }

    private void initialize() {
        boolean enabled = isEnabled();
        mInitializeTimestamp = enabled ? System.currentTimeMillis() : -1;

        if (!enabled) {
            if (mUncaughtExceptionHandler != null) {
                mUncaughtExceptionHandler.unregister();
            }
            return;
        }

        mUncaughtExceptionHandler = new UncaughtExceptionHandler(mDevice);
    }

    private void queuePendingCrashes() {
        // TODO Add the pending crashes to the corresponding log
    }

    protected long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

}
