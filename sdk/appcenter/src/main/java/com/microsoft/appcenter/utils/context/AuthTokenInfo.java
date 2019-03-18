/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import java.util.Date;

/**
 *
 */
public class AuthTokenInfo {

    private final String mAuthToken;

    private final Date mStartTime;

    private final Date mEndTime;

    public AuthTokenInfo(String authToken, Date startTime, Date endTime) {
        mAuthToken = authToken;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public Date getStartTime() {
        return mStartTime;
    }

    public Date getEndTime() {
        return mEndTime;
    }
}
