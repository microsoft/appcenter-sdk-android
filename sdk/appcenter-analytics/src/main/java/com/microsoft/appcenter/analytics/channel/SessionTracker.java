/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.channel;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.SessionContext;

import java.util.Date;
import java.util.UUID;

/**
 * Decorator for channel, adding session semantic to logs.
 */
public class SessionTracker extends AbstractChannelListener {

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
    }

    @Override
    public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {

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
        Date timestamp = log.getTimestamp();
        if (timestamp != null) {
            SessionContext.SessionInfo pastSession = SessionContext.getInstance().getSessionAt(timestamp.getTime());
            if (pastSession != null) {
                log.setSid(pastSession.getSessionId());
            }
        }

        /* If the log does not have a timestamp yet, then we just correlate with current session. */
        else {

            /* Set current session identifier. */
            log.setSid(mSid);

            /* Record queued time only if the log is using current session. */
            mLastQueuedLogTime = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Generate a new session identifier if the first time or
     * we went in background for more X seconds before resume or
     * if enough time has elapsed since the last background usage of the API.
     * <p>
     * Indeed the API can be used for events or crashes only for example, we need to renew
     * the session even when no pages are triggered but at the same time we want to keep using
     * the same session as long as the current activity is not paused (long video for example).
     */
    private void sendStartSessionIfNeeded() {
        if (mSid == null || hasSessionTimedOut()) {

            /* New session: generate a new identifier. */
            mSid = UUID.randomUUID();

            /* Update session storage. */
            SessionContext.getInstance().addSession(mSid);

            /*
             * Record queued time for the session log itself to avoid double log if resuming
             * from background after timeout and sending a log at same time we resume like a page.
             */
            mLastQueuedLogTime = SystemClock.elapsedRealtime();

            /* Enqueue a start session log. */
            StartSessionLog startSessionLog = new StartSessionLog();
            startSessionLog.setSid(mSid);
            mChannel.enqueue(startSessionLog, mGroupName, Flags.DEFAULTS);
        }
    }

    /**
     * Call this whenever an activity is resumed to update session tracker state.
     */
    public void onActivityResumed() {

        /* Record resume time for session timeout management. */
        AppCenterLog.debug(Analytics.LOG_TAG, "onActivityResumed");
        mLastResumedTime = SystemClock.elapsedRealtime();
        sendStartSessionIfNeeded();
    }

    /**
     * Call this whenever an activity is paused to update session tracker state.
     */
    public void onActivityPaused() {

        /* Record pause time for session timeout management. */
        AppCenterLog.debug(Analytics.LOG_TAG, "onActivityPaused");
        mLastPausedTime = SystemClock.elapsedRealtime();
    }

    /**
     * Clear storage from saved session state.
     */
    public void clearSessions() {
        SessionContext.getInstance().clearSessions();
    }

    /**
     * Check if current session has timed out.
     *
     * @return true if current session has timed out, false otherwise.
     */
    private boolean hasSessionTimedOut() {

        /*
         * Corner case: we have not been paused yet, typically we stayed on the first activity or
         * we are called from background (for example a broadcast intent that wakes up application,
         * new process).
         */
        if (mLastPausedTime == null) {
            return false;
        }

        /* Normal case: we saw both resume and paused events, compare all times. */
        long now = SystemClock.elapsedRealtime();
        boolean noLogSentForLong = now - mLastQueuedLogTime >= SESSION_TIMEOUT;
        boolean wasBackgroundForLong = mLastResumedTime - Math.max(mLastPausedTime, mLastQueuedLogTime) >= SESSION_TIMEOUT;
        AppCenterLog.debug(Analytics.LOG_TAG, "noLogSentForLong=" + noLogSentForLong + " wasBackgroundForLong=" + wasBackgroundForLong);
        return noLogSentForLong && wasBackgroundForLong;
    }
}
