/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

public class JwtClaims {

    private static final String JWT_PARTS_SEPARATOR_REGEX = "\\.";

    private static final String SUBJECT = "sub";

    private static final String EXPIRATION = "exp";

    private String mSubject;

    private Date mExpirationDate;

    @VisibleForTesting
    public JwtClaims(String subject, Date expirationDate) {
        mSubject = subject;
        mExpirationDate = expirationDate;
    }

    public static JwtClaims parse(String jwt) {
        String[] parts = jwt.split(JWT_PARTS_SEPARATOR_REGEX);
        if (parts.length < 2) {
            AppCenterLog.error(LOG_TAG, "Failed to parse JWT, not enough parts.");
            return null;
        }
        String base64ClaimsPart = parts[1];
        try {
            String claimsPart = new String(Base64.decode(base64ClaimsPart, Base64.DEFAULT));
            JSONObject claims = new JSONObject(claimsPart);
            String subject = claims.getString(SUBJECT);
            Date expirationDate = new Date(TimeUnit.SECONDS.toMillis(claims.getInt(EXPIRATION)));
            return new JwtClaims(subject, expirationDate);
        } catch (JSONException | IllegalArgumentException e) {
            AppCenterLog.error(LOG_TAG, "Failed to parse JWT", e);
            return null;
        }
    }

    public String getSubject() {
        return mSubject;
    }

    public Date getExpirationDate() {
        return mExpirationDate;
    }
}
