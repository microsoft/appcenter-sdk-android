package avalanche.analytics.channel;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import avalanche.analytics.ingestion.models.StartSessionLog;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.Log;
import avalanche.core.utils.AndroidTimeSource;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.TimeSource;
import avalanche.core.utils.UUIDUtils;

/**
 * Decorator for channel, adding session semantic to logs.
 */
public class SessionTracker implements AvalancheChannel.Listener {

    /**
     * Key used in storage to persist sessions.
     */
    public static final String STORAGE_KEY = "sessions";

    /**
     * Maximum number of sessions to persist the state.
     */
    private static final int STORAGE_MAX_SESSIONS = 5;

    /**
     * Separator used for persistent storage format.
     */
    private static final String STORAGE_KEY_VALUE_SEPARATOR = "/";

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
    private final long mSessionTimeout;

    /**
     * Time source.
     */
    private final TimeSource mTimeSource;

    /**
     * Past and current session identifiers sorted by session starting timestamp (ascending).
     */
    private final NavigableMap<Long, UUID> mSessions = new TreeMap<>();

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
        this(channel, SESSION_TIMEOUT, new AndroidTimeSource());
    }

    @VisibleForTesting
    SessionTracker(AvalancheChannel channel, long sessionTimeout, TimeSource timeSource) {
        mChannel = channel;
        mSessionTimeout = sessionTimeout;
        mTimeSource = timeSource;

        /* Try loading past sessions from storage. */
        try {
            Set<String> storedSessions = StorageHelper.PreferencesStorage.getStringSet(STORAGE_KEY);
            if (storedSessions != null) {
                for (String session : storedSessions) {
                    String[] split = session.split(STORAGE_KEY_VALUE_SEPARATOR);
                    if (split.length == 2) {
                        Long time = Long.parseLong(split[0]);
                        UUID sid = UUID.fromString(split[1]);
                        mSessions.put(time, sid);
                    }
                }
            }
            AvalancheLog.debug("Loaded stored sessions: " + mSessions);
        } catch (RuntimeException e) {
            AvalancheLog.warn("Failed to log stored sessions state.", e);
        }
    }

    @Override
    public synchronized void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {

        /* Since we enqueue start session logs, skip them to avoid infinite loop. */
        if (log instanceof StartSessionLog)
            return;

        /*
         * If the log has already specified a timestamp, try correlating with a past session.
         * Note that it can also find the current session but that's ok: in that case that means
         * its a log that will be associated to current session but won't trigger expiration logic.
         */
        if (log.getToffset() > 0) {
            Map.Entry<Long, UUID> pastSession = mSessions.lowerEntry(log.getToffset());
            if (pastSession != null)
                log.setSid(pastSession.getValue());
        }

        /* If the log is not correlated to a past session. */
        if (log.getSid() == null) {

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

                /* Update session map. */
                mSessions.put(mTimeSource.currentTimeMillis(), mSid);

                /* Remove oldest session if we reached maximum storage capacity. */
                if (mSessions.size() > STORAGE_MAX_SESSIONS)
                    mSessions.pollFirstEntry();

                /* Persist sessions. */
                Set<String> sessionStorage = new HashSet<>();
                for (Map.Entry<Long, UUID> session : mSessions.entrySet())
                    sessionStorage.add(session.getKey() + STORAGE_KEY_VALUE_SEPARATOR + session.getValue());
                StorageHelper.PreferencesStorage.putStringSet(STORAGE_KEY, sessionStorage);

                /* Enqueue a start session log. */
                StartSessionLog startSessionLog = new StartSessionLog();
                startSessionLog.setSid(mSid);
                mChannel.enqueue(startSessionLog, groupName);
            }

            /* Set current session identifier. */
            log.setSid(mSid);

            /* Record queued time only if the log is using current session. */
            mLastQueuedLogTime = mTimeSource.elapsedRealtime();
        }
    }

    public synchronized void onActivityResumed() {

        /* Record resume time for session timeout management. */
        mLastResumedTime = mTimeSource.elapsedRealtime();
    }

    public synchronized void onActivityPaused() {

        /* Record pause time for session timeout management. */
        mLastPausedTime = mTimeSource.elapsedRealtime();
    }

    /**
     * Check if current session has timed out.
     *
     * @return true if current session has timed out, false otherwise.
     */
    private boolean hasSessionTimedOut() {
        long now = mTimeSource.elapsedRealtime();
        boolean noLogSentForLong = now - mLastQueuedLogTime >= mSessionTimeout;
        boolean isBackgroundForLong = mLastPausedTime >= mLastResumedTime && now - mLastPausedTime >= mSessionTimeout;
        boolean wasBackgroundForLong = mLastResumedTime - mLastPausedTime >= mSessionTimeout;
        return noLogSentForLong && (isBackgroundForLong || wasBackgroundForLong);
    }
}
