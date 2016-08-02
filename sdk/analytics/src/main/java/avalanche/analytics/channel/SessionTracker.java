package avalanche.analytics.channel;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.UUID;

import avalanche.analytics.ingestion.models.StartSessionLog;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.Log;
import avalanche.core.utils.UUIDUtils;

/**
 * Decorator for channel, adding session semantic to logs.
 */
public class SessionTracker implements AvalancheChannel.Listener {

    /**
     * Default session timeout in milliseconds.
     */
    private static final long SESSION_TIMEOUT = 20000;

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
     * @param channel channel to decorate.
     */
    public SessionTracker(AvalancheChannel channel) {
        this(channel, SESSION_TIMEOUT);
    }

    @VisibleForTesting
    SessionTracker(AvalancheChannel channel, long sessionTimeout) {
        mChannel = channel;
        mSessionTimeout = sessionTimeout;
    }

    @Override
    public void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {

        /* Since we enqueue start session logs, skip them to avoid infinite loop. */
        if (log instanceof StartSessionLog)
            return;

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
            mSid = UUIDUtils.randomUUID();

            /* Enqueue a start session log. */
            StartSessionLog startSessionLog = new StartSessionLog();
            startSessionLog.setSid(mSid);
            mChannel.enqueue(startSessionLog, groupName);
        }

        /* Each log has a session identifier and device properties. */
        log.setSid(mSid);
        mLastQueuedLogTime = SystemClock.elapsedRealtime();
    }

    public void onActivityResumed() {

        /* Record resume time for session timeout management. */
        mLastResumedTime = SystemClock.elapsedRealtime();
    }

    public void onActivityPaused() {

        /* Record pause time for session timeout management. */
        mLastPausedTime = SystemClock.elapsedRealtime();
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
}
