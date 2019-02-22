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
     * The utc timestamp for a token becoming invalid.
     */
    @Expose
    @SerializedName(value = "ttl")
    private long ttl;

    /**
     * Get the partition value.
     *
     * @return the partition value
     */
    public String partition() {
        return this.partition;
    }

    /**
     * Get the ttl timestamp
     * @return the ttl value
     */
    public long ttl() { return this.ttl; }

    /**
     * Set the partition value.
     *
     * @param partition the partition value to set
     * @return the TokenResult object itself.
     */
    public TokenResult withPartition(String partition) {
        this.partition = partition;
        return this;
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
     * Set cosmos db account name.
     *
     * @param dbAccount the dbAccount value to set
     * @return the TokenResult object itself.
     */
    public TokenResult withDbAccount(String dbAccount) {
        this.dbAccount = dbAccount;
        return this;
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
     * Set cosmos db database name within the specified account.
     *
     * @param dbName the dbName value to set
     * @return the TokenResult object itself.
     */
    public TokenResult withDbName(String dbName) {
        this.dbName = dbName;
        return this;
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
     * Set cosmos db collection name within the specified database.
     *
     * @param dbCollectionName the dbCollectionName value to set
     * @return the TokenResult object itself.
     */
    public TokenResult withDbCollectionName(String dbCollectionName) {
        this.dbCollectionName = dbCollectionName;
        return this;
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
     * Set the token to be used to talk to cosmos db.
     *
     * @param token the token value to set
     * @return the TokenResult object itself.
     */
    public TokenResult withToken(String token) {
        this.token = token;
        return this;
    }

    /**
     * Get possible values include: 'failed', 'unauthenticated', 'succeed'.
     *
     * @return the status value
     */
    public String status() {
        return this.status;
    }

    /**
     * Set possible values include: 'failed', 'unauthenticated', 'succeed'.
     *
     * @param status the status value to set
     * @return the TokenResult object itself.
     */
    public TokenResult withStatus(String status) {
        this.status = status;
        return this;
    }

}