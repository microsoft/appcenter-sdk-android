package com.microsoft.appcenter.storage.models;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Token fetch result.
 */
public class TokenResult {
    /**
     * The partition property.
     */
    @Expose
    @SerializedName(value = "partition")
    private String partition;

    /**
     * Cosmos db account name.
     */
    @Expose
    @SerializedName(value = "dbAccount")
    private String dbAccount;

    /**
     * Cosmos db database name within the specified account.
     */
    @Expose
    @SerializedName(value = "dbName")
    private String dbName;

    /**
     * Cosmos db collection name within the specified database.
     */
    @Expose
    @SerializedName(value = "dbCollectionName")
    private String dbCollectionName;

    /**
     * The token to be used to talk to cosmos db.
     */
    @Expose
    @SerializedName(value = "token")
    private String token;

    /**
     * Possible values include: 'failed', 'unauthenticated', 'succeed'.
     */
    @Expose
    @SerializedName(value = "status")
    private String status;

    /**
     * Get the partition value.
     *
     * @return the partition value
     */
    public String partition() {
        return this.partition;
    }

    /**
     * Get cosmos db account name.
     *
     * @return the dbAccount value
     */
    public String dbAccount() {
        return this.dbAccount;
    }

    /**
     * Get cosmos db database name within the specified account.
     *
     * @return the dbName value
     */
    public String dbName() {
        return this.dbName;
    }

    /**
     * Get cosmos db collection name within the specified database.
     *
     * @return the dbCollectionName value
     */
    public String dbCollectionName() {
        return this.dbCollectionName;
    }

    /**
     * Get the token to be used to talk to cosmos db.
     *
     * @return the token value
     */
    public String token() {
        return this.token;
    }

    /**
     * Get possible values include: 'failed', 'unauthenticated', 'succeed'.
     *
     * @return the status value
     */
    public String status() {
        return this.status;
    }
}