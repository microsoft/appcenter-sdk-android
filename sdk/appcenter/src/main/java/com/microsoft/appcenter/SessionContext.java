package com.microsoft.appcenter;

import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

/**
 * Persistent session history.
 */
public class SessionContext {

    /**
     * Key used in storage to persist sessions.
     */
    private static final String STORAGE_KEY = "sessions";

    /**
     * Maximum number of sessions to persist the state.
     */
    private static final int STORAGE_MAX_SESSIONS = 10;

    /**
     * Separator used for persistent storage format.
     * We store session timestamp, then session uuid (can be empty for the special launch session),
     * then the app launch timestamp. Each field just is separated by this character.
     */
    private static final String STORAGE_KEY_VALUE_SEPARATOR = "/";

    /**
     * Singleton.
     */
    private static SessionContext sInstance;

    /**
     * Past and current session identifiers sorted by session starting timestamp (ascending).
     */
    private final NavigableMap<Long, SessionInfo> mSessions = new TreeMap<>();

    /**
     * App launch timestamp. We could use the real process start time and not SDK start time.
     * But there is no Android API to do that it requires executing ps command or reading proc files.
     * This is used to know minidump files app launch timestamp that are processed after restart.
     * This is not used for regular managed crashes where that timestamp is maintained by Crashes
     * and has the same limitation (initialized at SDK start time).
     */
    private final long mAppLaunchTimestamp;

    /**
     * Init.
     */
    @WorkerThread
    private SessionContext() {

        /* Try loading past sessions from storage. */
        mAppLaunchTimestamp = System.currentTimeMillis();
        Set<String> storedSessions = StorageHelper.PreferencesStorage.getStringSet(STORAGE_KEY);
        if (storedSessions != null) {
            for (String session : storedSessions) {
                String[] split = session.split(STORAGE_KEY_VALUE_SEPARATOR, -1);
                try {
                    long time = Long.parseLong(split[0]);
                    String rawSid = split[1];
                    UUID sid = rawSid.isEmpty() ? null : UUID.fromString(rawSid);
                    long appLaunchTimestamp;
                    if (split.length > 2) {
                        appLaunchTimestamp = Long.parseLong(split[2]);
                    } else {

                        /* Backward compatibility with older SDK storage. Use placeholder. */
                        appLaunchTimestamp = time;
                    }
                    mSessions.put(time, new SessionInfo(time, sid, appLaunchTimestamp));
                } catch (RuntimeException e) {
                    AppCenterLog.warn(LOG_TAG, "Ignore invalid session in store: " + session, e);
                }
            }
        }
        AppCenterLog.debug(LOG_TAG, "Loaded stored sessions: " + mSessions);

        /*
         * Record a session with no identifier
         * to avoid correlating log to a session from a previous process.
         */
        addSession(null);
    }

    @WorkerThread
    public static synchronized SessionContext getInstance() {
        if (sInstance == null) {
            sInstance = new SessionContext();
        }
        return sInstance;
    }

    @VisibleForTesting
    public static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Record a new session in storage.
     * If maximum capacity of storage has been reached, the oldest session is discarded.
     *
     * @param sessionId session identifier.
     */
    public synchronized void addSession(UUID sessionId) {

        /* Update session map. */
        long now = System.currentTimeMillis();
        mSessions.put(now, new SessionInfo(now, sessionId, mAppLaunchTimestamp));

        /* Remove oldest session if we reached maximum storage capacity. */
        if (mSessions.size() > STORAGE_MAX_SESSIONS) {
            mSessions.pollFirstEntry();
        }

        /* Persist sessions. */
        Set<String> sessionStorage = new LinkedHashSet<>();
        for (SessionInfo session : mSessions.values()) {
            sessionStorage.add(session.toString());
        }
        StorageHelper.PreferencesStorage.putStringSet(STORAGE_KEY, sessionStorage);
    }

    /**
     * Get what was the current session from storage at the specified timestamp.
     *
     * @param timestamp try to find session at that timestamp.
     * @return found session or null.
     */
    public synchronized SessionInfo getSessionAt(long timestamp) {
        Map.Entry<Long, SessionInfo> pastEntry = mSessions.floorEntry(timestamp);
        if (pastEntry != null) {
            return pastEntry.getValue();
        }
        return null;
    }

    /**
     * Clear storage from saved session state.
     */
    public synchronized void clearSessions() {
        mSessions.clear();
        StorageHelper.PreferencesStorage.remove(STORAGE_KEY);
    }

    /**
     * Session information object.
     */
    public static class SessionInfo {

        /**
         * Session timestamp.
         */
        private final long mTimestamp;

        /**
         * Session identifier.
         */
        private final UUID mSessionId;

        /**
         * App launch timestamp.
         */
        private final long mAppLaunchTimestamp;

        /**
         * Init.
         *
         * @param timestamp          session timestamp.
         * @param sessionId          session identifier.
         * @param appLaunchTimestamp app launch timestamp.
         */
        SessionInfo(long timestamp, UUID sessionId, long appLaunchTimestamp) {
            mTimestamp = timestamp;
            mSessionId = sessionId;
            mAppLaunchTimestamp = appLaunchTimestamp;
        }

        /**
         * @return session timestamp.
         */
        @SuppressWarnings("WeakerAccess")
        public long getTimestamp() {
            return mTimestamp;
        }

        /**
         * @return session identifier.
         */
        public UUID getSessionId() {
            return mSessionId;
        }

        /**
         * @return application launch timestamp.
         */
        public long getAppLaunchTimestamp() {
            return mAppLaunchTimestamp;
        }

        @Override
        public String toString() {
            String rawSession = getTimestamp() + STORAGE_KEY_VALUE_SEPARATOR;
            if (getSessionId() != null) {
                rawSession += getSessionId();
            }
            rawSession += STORAGE_KEY_VALUE_SEPARATOR + getAppLaunchTimestamp();
            return rawSession;
        }
    }
}
