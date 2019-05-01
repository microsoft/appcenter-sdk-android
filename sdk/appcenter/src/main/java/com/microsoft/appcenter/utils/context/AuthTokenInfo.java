/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import java.util.Calendar;
import java.util.Date;

/**
 * Auth token information object.
 */
public class AuthTokenInfo {

    /**
     * Time till expiration (in seconds) when we assume token is about to expire and can refresh token.
     */
    private static final int EXPIRATION_OFFSET_TO_REFRESH_SEC = 10 * 60;

    /**
     * Auth token (<code>null</code> for anonymous).
     */
    private final String mAuthToken;

    /**
     * The time from which the token began to act.
     * It can be <code>null</code> if it applies to all logs before {@link #mEndTime}.
     */
    private final Date mStartTime;

    /**
     * The time to which the token acted.
     * It can be <code>null</code> if it's still valid.
     */
    private final Date mEndTime;

    /**
     * Init.
     */
    public AuthTokenInfo() {
        this(null, null, null);
    }

    /**
     * Init.
     *
     * @param authToken auth token.
     * @param startTime the time from which the token began to act.
     * @param endTime   the time to which the token acted.
     */
    public AuthTokenInfo(String authToken, Date startTime, Date endTime) {
        mAuthToken = authToken;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    /**
     * Returns true if token is about to expire.
     *
     * @return boolean, true if expires soon.
     */
    boolean isAboutToExpire() {
        if (mEndTime == null) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, EXPIRATION_OFFSET_TO_REFRESH_SEC);
        return calendar.getTime().after(mEndTime);
    }

    /**
     * @return auth token.
     */
    public String getAuthToken() {
        return mAuthToken;
    }

    /**
     * @return the time from which the token began to act.
     */
    public Date getStartTime() {
        return mStartTime;
    }

    /**
     * @return the time to which the token acted.
     */
    public Date getEndTime() {
        return mEndTime;
    }
}
