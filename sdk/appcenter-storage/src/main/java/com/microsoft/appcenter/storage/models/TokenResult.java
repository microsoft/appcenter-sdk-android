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
     * The UTC timestamp for a token expiration time.
     */
    @Expose
    @SerializedName(value = "expiresOn")
    private String expiresOn;

    /**
     * Get the partition value.
     *
     * @return The partition value.
     */
    public String partition() {
        return this.partition;
    }

    /**
     * Get the token expiration time.
     *
     * @return The token expiration value.
     */
    public Date expiresOn() {
        try {
            return JSONDateUtils.toDate(this.expiresOn);
        } catch (JSONException ex) {
            AppCenterLog.error(
                    LOG_TAG, String.format(
                            "Unable to convert '%s' to ISO 8601 Date format ",
                            expiresOn));
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
        this.partition = partition;
        return this;
    }

    /**
     * Set the token expiration time value.
     *
     * @param expiresOn Token expiration time value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withExpirationTime(Date expiresOn) {
        try {
            this.expiresOn = JSONDateUtils.toString(expiresOn);
        } catch (JSONException ex) {
            AppCenterLog.error(LOG_TAG, "Unable to convert null Date to ISO 8601 string");
            this.expiresOn = null;
        }
        return this;
    }

    /**
     * Get cosmos db account name.
     *
     * @return The dbAccount value.
     */
    public String dbAccount() {
        return this.dbAccount;
    }

    /**
     * Set cosmos db account name.
     *
     * @param dbAccount The dbAccount value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withDbAccount(String dbAccount) {
        this.dbAccount = dbAccount;
        return this;
    }

    /**
     * Get cosmos db database name within the specified account.
     *
     * @return The dbName value.
     */
    public String dbName() {
        return this.dbName;
    }

    /**
     * Set cosmos db database name within the specified account.
     *
     * @param dbName The dbName value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * Get cosmos db collection name within the specified database.
     *
     * @return The dbCollectionName value.
     */
    public String dbCollectionName() {
        return this.dbCollectionName;
    }

    /**
     * Set cosmos db collection name within the specified database.
     *
     * @param dbCollectionName The dbCollectionName value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withDbCollectionName(String dbCollectionName) {
        this.dbCollectionName = dbCollectionName;
        return this;
    }

    /**
     * Get the token to be used to talk to cosmos db.
     *
     * @return The token value.
     */
    public String token() {
        return this.token;
    }

    /**
     * Set the token to be used to talk to cosmos db.
     *
     * @param token The token value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withToken(String token) {
        this.token = token;
        return this;
    }

    /**
     * Get possible values include: 'failed', 'unauthenticated', 'succeed'.
     *
     * @return The status value.
     */
    public String status() {
        return this.status;
    }

    /**
     * Set possible values include: 'failed', 'unauthenticated', 'succeed'.
     *
     * @param status The status value to set.
     * @return The TokenResult object itself.
     */
    public TokenResult withStatus(String status) {
        this.status = status;
        return this;
    }
}