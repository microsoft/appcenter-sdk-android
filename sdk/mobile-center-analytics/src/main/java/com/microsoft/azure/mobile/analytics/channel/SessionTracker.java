package com.microsoft.azure.mobile.analytics.channel;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.analytics.ingestion.models.StartSessionLog;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.StartServiceLog;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Decorator for channel, adding session semantic to logs.
 */
public class SessionTracker implements Channel.Listener {

    /**
     * Key used in storage to persist sessions.
     */
    private static final String STORAGE_KEY = "sessions";

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
    private final Channel mChannel;

    /**
     * Group name used to send generated logs.
     */
    private final String mGroupName;

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
     * This value is null when the application resume event has not yet been seen.
     */
    private Long mLastResumedTime;

    /**
     * Timestamp of the last time the application went to background.
     * This value is null when the application pause event has not yet been seen.
     */
    private Long mLastPausedTime;

    /**
     * Init.
     *
     * @param channel   channel to decorate.
     * @param groupName group name used to send generated logs.
     */
    public SessionTracker(Channel channel, String groupName) {
        mChannel = channel;
        mGroupName = groupName;

        /* Try loading past sessions from storage. */
        Set<String> storedSessions = StorageHelper.PreferencesStorage.getStringSet(STORAGE_KEY);
        if (storedSessions != null) {
            for (String session : storedSessions) {
                String[] split = session.split(STORAGE_KEY_VALUE_SEPARATOR);
                try {
                    Long time = Long.parseLong(split[0]);
                    UUID sid = UUID.fromString(split[1]);
                    mSessions.put(time, sid);
                } catch (RuntimeException e) {
                    MobileCenterLog.warn(Analytics.LOG_TAG, "Ignore invalid session in store: " + session, e);
                }
            }
        }
        MobileCenterLog.debug(Analytics.LOG_TAG, "Loaded stored sessions: " + mSessions);
    }

    @Override
    public synchronized void onEnqueuingLog(@NonNull Log log, @NonNull String groupName) {

        /*
         * Since we enqueue start session logs, skip them to avoid infinite loop.
         * Also skip start service log as it's always sent and should not trigger a session.
         */
        if (log instanceof StartSessionLog || log instanceof StartServiceLog) {
            return;
        }

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

            /* Send a new start session log if needed. */
            sendStartSessionIfNeeded();

            /* Set current session identifier. */
            log.setSid(mSid);

            /* Record queued time only if the log is using current session. */
            mLastQueuedLogTime = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Generate a new session identifier if the first time or
     * we went in background for more X seconds or
     * if enough time has elapsed since the last background usage of the API.
     * <p>
     * Indeed the API can be used for events or crashes only for example, we need to renew
     * the session even when no pages are triggered but at the same time we want to keep using
     * the same session as long as the current activity is not paused (long video for example).
     */
    private void sendStartSessionIfNeeded() {
        if (mSid == null || hasSessionTimedOut()) {

            /* New session: generate a new identifier. */
            mSid = UUIDUtils.randomUUID();

            /* Update session map. */
            mSessions.put(System.currentTimeMillis(), mSid);

            /* Remove oldest session if we reached maximum storage capacity. */
            if (mSessions.size() > STORAGE_MAX_SESSIONS)
                mSessions.pollFirstEntry();

            /* Persist sessions. */
            Set<String> sessionStorage = new HashSet<>();
            for (Map.Entry<Long, UUID> session : mSessions.entrySet())
                sessionStorage.add(session.getKey() + STORAGE_KEY_VALUE_SEPARATOR + session.getValue());
            StorageHelper.PreferencesStorage.putStringSet(STORAGE_KEY, sessionStorage);

            /*
             * Record queued time for the session log itself to avoid double log if resuming
             * from background after timeout and sending a log at same time we resume like a page.
             */
            mLastQueuedLogTime = SystemClock.elapsedRealtime();

            /* Enqueue a start session log. */
            StartSessionLog startSessionLog = new StartSessionLog();
            startSessionLog.setSid(mSid);
            mChannel.enqueue(startSessionLog, mGroupName);
        }
    }

    /**
     * Call this whenever an activity is resumed to update session tracker state.
     */
    public synchronized void onActivityResumed() {

        /* Record resume time for session timeout management. */
        MobileCenterLog.debug(Analytics.LOG_TAG, "onActivityResumed");
        mLastResumedTime = SystemClock.elapsedRealtime();
        sendStartSessionIfNeeded();
    }

    /**
     * Call this whenever an activity is paused to update session tracker state.
     */
    public synchronized void onActivityPaused() {

        /* Record pause time for session timeout management. */
        MobileCenterLog.debug(Analytics.LOG_TAG, "onActivityPaused");
        mLastPausedTime = SystemClock.elapsedRealtime();
    }

    /**
     * Clear storage from saved session state.
     */
    public synchronized void clearSessions() {
        StorageHelper.PreferencesStorage.remove(STORAGE_KEY);
    }

    /**
     * Check if current session has timed out.
     *
     * @return true if current session has timed out, false otherwise.
     */
    private boolean hasSessionTimedOut() {

        /* Compute how long we have not sent a log. */
        long now = SystemClock.elapsedRealtime();
        boolean noLogSentForLong = now - mLastQueuedLogTime >= SESSION_TIMEOUT;

        /* Corner case: we have not been paused yet, typically we stayed on the first activity or we are called from background (for example a broadcast intent that wakes up application, new process). */
        if (mLastPausedTime == null) {

            /* If we saw a resume in event, we are in foreground, so no expiration. If we are in background, check how long. */
            return mLastResumedTime == null && noLogSentForLong;
        }

        /* Corner case 2: we saw a pause but not a resume event: we are in background, check how long. */
        if (mLastResumedTime == null) {

            /* Note that this corner case is likely an integration issue. It's not supposed to happen. Likely the SDK has been configured too late. */
            return noLogSentForLong;
        }

        /* Normal case: we saw both resume and paused events, compare all times. */
        boolean isBackgroundForLong = mLastPausedTime >= mLastResumedTime && now - mLastPausedTime >= SESSION_TIMEOUT;
        boolean wasBackgroundForLong = mLastResumedTime - Math.max(mLastPausedTime, mLastQueuedLogTime) >= SESSION_TIMEOUT;
        MobileCenterLog.debug(Analytics.LOG_TAG, "noLogSentForLong=" + noLogSentForLong + " isBackgroundForLong=" + isBackgroundForLong + " wasBackgroundForLong=" + wasBackgroundForLong);
        return noLogSentForLong && (isBackgroundForLong || wasBackgroundForLong);
    }
}
