/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Date;

final class AuthTokenHistoryEntry implements Model {

    private static final String AUTH_TOKEN = "authToken";

    private static final String HOME_ACCOUNT_ID = "homeAccountId";

    private static final String TIME = "time";

    private static final String EXPIRES_ON = "expiresOn";

    private String mAuthToken;

    private String mHomeAccountId;

    private Date mTime;

    private Date mExpiresOn;

    AuthTokenHistoryEntry() {
    }

    AuthTokenHistoryEntry(String authToken, String homeAccountId, Date time, Date expiresOn) {
        mAuthToken = authToken;
        mHomeAccountId = homeAccountId;
        mTime = time;
        mExpiresOn = expiresOn;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    private void setAuthToken(String authToken) {
        mAuthToken = authToken;
    }

    String getHomeAccountId() {
        return mHomeAccountId;
    }

    private void setHomeAccountId(String homeAccountId) {
        mHomeAccountId = homeAccountId;
    }

    public Date getTime() {
        return mTime;
    }

    private void setTime(Date time) {
        mTime = time;
    }

    Date getExpiresOn() {
        return mExpiresOn;
    }

    private void setExpiresOn(Date expiresOn) {
        mExpiresOn = expiresOn;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setAuthToken(object.optString(AUTH_TOKEN, null));
        setHomeAccountId(object.optString(HOME_ACCOUNT_ID, null));
        String time = object.optString(TIME, null);
        setTime(time != null ? JSONDateUtils.toDate(time) : null);
        String expiresOn = object.optString(EXPIRES_ON, null);
        setExpiresOn(expiresOn != null ? JSONDateUtils.toDate(expiresOn) : null);
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, AUTH_TOKEN, getAuthToken());
        JSONUtils.write(writer, HOME_ACCOUNT_ID, getHomeAccountId());
        Date time = getTime();
        JSONUtils.write(writer, TIME, time != null ? JSONDateUtils.toString(time) : null);
        Date expiresOn = getExpiresOn();
        JSONUtils.write(writer, EXPIRES_ON, expiresOn != null ? JSONDateUtils.toString(expiresOn) : null);
    }
}
