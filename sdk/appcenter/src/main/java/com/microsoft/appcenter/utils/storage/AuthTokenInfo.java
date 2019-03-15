/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import java.util.Date;

/**
 *
 */
public class AuthTokenInfo {

    private final String mToken;

    private final Date mStartTime;

    private final Date mEndTime;

    public AuthTokenInfo(String token, Date startTime, Date endTime) {
        mToken = token;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public String getToken() {
        return mToken;
    }

    public Date getStartTime() {
        return mStartTime;
    }

    public Date getEndTime() {
        return mEndTime;
    }
}
