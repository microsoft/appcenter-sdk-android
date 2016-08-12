package avalanche.errors;

import android.content.Context;
import android.support.annotation.NonNull;

import org.json.JSONException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import avalanche.core.AbstractAvalancheFeature;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.StorageHelper;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.errors.model.TestCrashException;
import avalanche.errors.utils.ErrorLogHelper;


public class ErrorReporting extends AbstractAvalancheFeature {

    private static final String ERROR_GROUP = "group_error";

    private static ErrorReporting sInstance = null;

    private final Map<String, LogFactory> mFactories;

    private final LogSerializer mLogSerializer;

    private Context mContext;

    private long mInitializeTimestamp;

    private UncaughtExceptionHandler mUncaughtExceptionHandler;

    private ErrorReporting() {
        mFactories = new HashMap<>();
        mFactories.put(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());
    }

    @NonNull
    public static synchronized ErrorReporting getInstance() {
        if (sInstance == null) {
            sInstance = new ErrorReporting();
        }
        return sInstance;
    }

    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Generates crash for test purpose.
     */
    public static void generateTestCrash() {
        throw new TestCrashException();
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        initialize();
    }

    @Override
    public synchronized void onChannelReady(@NonNull Context context, @NonNull AvalancheChannel channel) {
        super.onChannelReady(context, channel);
        mContext = context;
        initialize();
        if (isInstanceEnabled() && mChannel != null) {
            queuePendingCrashes();
        }
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    @Override
    protected String getGroupName() {
        return ERROR_GROUP;
    }

    private void initialize() {
        boolean enabled = isInstanceEnabled();
        mInitializeTimestamp = enabled ? System.currentTimeMillis() : -1;
        if (!enabled) {
            if (mUncaughtExceptionHandler != null) {
                mUncaughtExceptionHandler.unregister();
                mUncaughtExceptionHandler = null;
            }
        } else if (mContext != null) {
            mUncaughtExceptionHandler = new UncaughtExceptionHandler(mContext);
        }
    }

    private void queuePendingCrashes() {
        for (File logfile : ErrorLogHelper.getStoredErrorLogFiles()) {
            String logfileContents = StorageHelper.InternalStorage.read(logfile);
            AvalancheLog.info("Deleting error log file " + logfile.getName());
            StorageHelper.InternalStorage.delete(logfile);
            try {
                JavaErrorLog log = (JavaErrorLog) mLogSerializer.deserializeLog(logfileContents);
                if (log != null) {
                    mChannel.enqueue(log, ERROR_GROUP);
                }
            } catch (JSONException e) {
                AvalancheLog.error("Error parsing error log", e);
            }
        }
    }

    synchronized long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

}
