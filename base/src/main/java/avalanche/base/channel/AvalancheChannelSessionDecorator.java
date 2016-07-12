package avalanche.base.channel;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.UUID;

import avalanche.base.ingestion.models.Device;
import avalanche.base.ingestion.models.Log;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.DeviceInfoHelper;

/**
 * Decorator for channel, adding session semantic to logs.
 */
public class AvalancheChannelSessionDecorator implements AvalancheChannel, Application.ActivityLifecycleCallbacks {

    /**
     * Default session timeout in milliseconds.
     */
    private static final long SESSION_TIMEOUT = 20000;

    /**
     * Application context.
     */
    private final Context mContext;

    /**
     * Decorated channel.
     */
    private final AvalancheChannel mChannel;

    /**
     * Session timeout.
     */
    private long mSessionTimeout = SESSION_TIMEOUT;

    /**
     * Current session identifier.
     */
    private UUID mSid;

    /**
     * Device properties for the current session.
     */
    private Device mDevice;

    /**
     * Timestamp of the last log queued to channel.
     */
    private long mLastQueuedLogTime;

    /**
     * Timestamp of the last time the application went to foreground.
     */
    private long mLastResumedTime;

    /**
     * Timestamp of the last time the application went to background.
     */
    private long mLastPausedTime;

    /**
     * Init.
     *
     * @param context any context.
     * @param channel channel to decorate.
     */
    public AvalancheChannelSessionDecorator(Context context, AvalancheChannel channel) {
        mContext = context.getApplicationContext();
        mChannel = channel;
    }

    /**
     * Set session timeout.
     *
     * @param sessionTimeout session timeout.
     */
    @VisibleForTesting
    void setSessionTimeout(long sessionTimeout) {
        mSessionTimeout = sessionTimeout;
    }

    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {

        /*
         * Generate a new session identifier if the first time or
         * we went in background for more X seconds or
         * if enough time has elapsed since the last background usage of the API.
         *
         * Indeed the API can be used for events or crashes only for example, we need to renew
         * the session even when no pages are triggered but at the same time we want to keep using
         * the same session as long as the current activity is not paused (long video for example).
         */
        if (mSid == null || hasSessionTimedOut()) {

            /* New session: generate a new identifier. */
            mSid = UUID.randomUUID();

            /* And generate a new device property bag, keep the same for all the session duration. */
            try {
                mDevice = DeviceInfoHelper.getDeviceInfo(mContext);
            } catch (DeviceInfoHelper.DeviceInfoException e) {
                AvalancheLog.error("Device log cannot be generated", e);
                return;
            }
        }

        /* Each log has a session identifier and device properties. */
        log.setSid(mSid);
        log.setDevice(mDevice);
        // TODO log.setToffset(now); // use absolute time for persistence, will be converted before sending

        /* Forward decorated log to channel, record queue time for session timeout management. */
        mChannel.enqueue(log, queueName);
        mLastQueuedLogTime = SystemClock.elapsedRealtime();
    }

    /**
     * Check if current session has timed out.
     *
     * @return true if current session has timed out, false otherwise.
     */
    private boolean hasSessionTimedOut() {
        long now = SystemClock.elapsedRealtime();
        boolean noLogSentForLong = now - mLastQueuedLogTime >= mSessionTimeout;
        boolean isBackgroundForLong = mLastPausedTime >= mLastResumedTime && now - mLastPausedTime >= mSessionTimeout;
        boolean wasBackgroundForLong = mLastResumedTime - mLastPausedTime >= mSessionTimeout;
        return noLogSentForLong && (isBackgroundForLong || wasBackgroundForLong);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {

        /* Record resume time for session timeout management. */
        mLastResumedTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onActivityPaused(Activity activity) {

        /* Record pause time for session timeout management. */
        mLastPausedTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
