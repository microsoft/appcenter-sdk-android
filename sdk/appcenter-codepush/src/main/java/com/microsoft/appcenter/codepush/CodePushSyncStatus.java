package com.microsoft.appcenter.codepush.enums;

import com.google.gson.annotations.SerializedName;

/**
 * A enum defining the status of the synchronization process.
 */
public enum CodePushSyncStatus {

    /**
     * Appliccation is up to date.
     */
    @SerializedName("0")
    UP_TO_DATE(0),

    /**
     * An update has been installed.
     */
    @SerializedName("1")
    UPDATE_INSTALLED(1),

    /**
     * An update has been ignored by user.
     */
    @SerializedName("2")
    UPDATE_IGNORED(2),

    /**
     * Error during synchronization.
     */
    @SerializedName("3")
    UNKNOWN_ERROR(3),

    /**
     * Synchronization is in progess.
     */
    @SerializedName("4")
    SYNC_IN_PROGRESS(4),

    /**
     * Application is checking for update.
     */
    @SerializedName("5")
    CHECKING_FOR_UPDATE(5),

    /**
     * Applciation waits for user to respond what to do with update.
     */
    @SerializedName("6")
    AWAITING_USER_ACTION(6),

    /**
     * Application downloads the update package.
     */
    @SerializedName("7")
    DOWNLOADING_PACKAGE(7),

    /**
     * Application is installing an update.
     */
    @SerializedName("8")
    INSTALLING_UPDATE(8);

    private final int value;

    CodePushSyncStatus(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return this.value;
    }
}