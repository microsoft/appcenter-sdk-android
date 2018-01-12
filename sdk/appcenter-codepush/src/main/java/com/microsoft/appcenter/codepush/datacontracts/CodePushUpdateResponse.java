package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * A response class containing info about the update.
 */
public class CodePushUpdateResponse {

    /**
     * Information about the existing update.
     */
    @SerializedName("updateInfo")
    private CodePushUpdateResponseUpdateInfo updateInfo;

    /**
     * Gets the value of updateInfo and returns it.
     *
     * @return updateInfo.
     */
    public CodePushUpdateResponseUpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    /**
     * Sets the updateInfo.
     *
     * @param updateInfo new value.
     */
    public void setUpdateInfo(CodePushUpdateResponseUpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }
}
