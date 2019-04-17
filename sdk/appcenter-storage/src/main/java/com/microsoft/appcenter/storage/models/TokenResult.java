/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;

import java.util.Date;

import static com.microsoft.appcenter.storage.Constants.LOG_TAG;

/**
 * Token fetch result.
 */
public class TokenResult {

    /**
     * The partition property.
     */
    @Expose
    @SerializedName(value = "partition")
    private String mPartition;

    /**
     * Cosmos db account name.
     */
    @Expose
    @SerializedName(value = "dbAccount")
    private String mDbAccount;

    /**
     * Cosmos db database name within the specified account.
     */
    @Expose
    @SerializedName(value = "dbName")
    private String mDbName;

    /**
     * Cosmos db collection name within the specified database.
     */
    @Expose
    @SerializedName(value = "dbCollectionName")
    private String mDbCollectionName;

    /**
     * The token to be used to talk to cosmos db.
     */
    @Expose
    @SerializedName(value = "token")
    private String mToken;

    /**
     * Possible values include: 'failed', 'unauthenticated', 'succeed'.
     */
    @Expose
    @SerializedName(value = "status")
    private String mStatus;

    /**
     * The UTC timestamp for a token expiration time.
     */
    @Expose
    @SerializedName(value = "expiresOn")
    private String mExpirationDate;

    /**
     * The account id.
     */
    @Expose
    @SerializedName(value = "accountId")
    private String mAccountId;

    /**
     * Get the partition value.
     *
     * @return The partition value.
     */
    public String getPartition() {
        return mPartition;
    }

    /**
     * Get the token expiration date.
     *
     * @return The token expiration date value.
     */
    public Date getExpirationDate() {
        try {
            return JSONDateUtils.toDate(mExpirationDate);
        } catch (JSONException ex) {
            AppCenterLog.error(
                    LOG_TAG, String.format(
                            "Unable to convert '%s' to ISO 8601 Date format ",
                            mExpirationDate));
            return new Date(0);
        }
    }

    /**
     * Set the partition value.
     *
     * @param partition The partition value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withPartition(String partition) {
        mPartition = partition;
        return this;
    }

    /**
     * Set the token expiration time value.
     *
     * @param expiresOn Token expiration time value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withExpirationDate(Date expiresOn) {
        try {
            mExpirationDate = JSONDateUtils.toString(expiresOn);
        } catch (JSONException e) {
            AppCenterLog.error(LOG_TAG, "Unable to convert null Date to ISO 8601 string", e);
            mExpirationDate = null;
        }
        return this;
    }

    /**
     * Set the account id.
     *
     * @param accountId Account id value to be set.
     * @return The TokenResult object itself.
     */
    public TokenResult withAccountId(String accountId) {
        mAccountId = accountId;
        return this;
    }

    /**
     * Get cosmos db account name.
     *
     * @return The dbAccount value.
     */
    public String getDbAccount() {
        return mDbAccount;
    }

    /**
     * Set cosmos db account name.
     *
     * @param dbAccount The dbAccount value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withDbAccount(String dbAccount) {
        mDbAccount = dbAccount;
        return this;
    }

    /**
     * Get cosmos db database name within the specified account.
     *
     * @return The dbName value.
     */
    public String getDbName() {
        return mDbName;
    }

    /**
     * Set cosmos db database name within the specified account.
     *
     * @param dbName The dbName value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withDbName(String dbName) {
        mDbName = dbName;
        return this;
    }

    /**
     * Get cosmos db collection name within the specified database.
     *
     * @return The database collection name.
     */
    public String getDbCollectionName() {
        return mDbCollectionName;
    }

    /**
     * Set cosmos db collection name within the specified database.
     *
     * @param dbCollectionName The database collection name value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withDbCollectionName(String dbCollectionName) {
        mDbCollectionName = dbCollectionName;
        return this;
    }

    /**
     * Get the token to be used to talk to cosmos db.
     *
     * @return The token value.
     */
    public String getToken() {
        return mToken;
    }

    /**
     * Set the token to be used to talk to cosmos db.
     *
     * @param token The token value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withToken(String token) {
        mToken = token;
        return this;
    }

    /**
     * Get possible values include: 'failed', 'unauthenticated', 'succeed'.
     *
     * @return The status value.
     */
    public String getStatus() {
        return mStatus;
    }

    /**
     * Get account id.
     *
     * @return The account id value.
     */
    public String getAccountId() {
        return mAccountId;
    }

    /**
     * Set possible values include: 'failed', 'unauthenticated', 'succeed'.
     *
     * @param status The status value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withStatus(String status) {
        mStatus = status;
        return this;
    }
}