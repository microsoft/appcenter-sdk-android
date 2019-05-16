/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

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
    private Date mExpirationDate;

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
        return mExpirationDate;
    }

    /**
     * Set the partition value.
     *
     * @param partition The partition value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult setPartition(String partition) {
        mPartition = partition;
        return this;
    }

    /**
     * Set the token expiration time value.
     *
     * @param expirationDate Token expiration time value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult setExpirationDate(Date expirationDate) {
        mExpirationDate = expirationDate;
        return this;
    }

    /**
     * Set the account id.
     *
     * @param accountId Account id value to be set.
     * @return The TokenResult object itself.
     */
    public TokenResult setAccountId(String accountId) {
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
    public TokenResult setDbAccount(String dbAccount) {
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
    public TokenResult setDbName(String dbName) {
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
    public TokenResult setDbCollectionName(String dbCollectionName) {
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
    public TokenResult setToken(String token) {
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
    public TokenResult setStatus(String status) {
        mStatus = status;
        return this;
    }
}