package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.codepush.CodePush;
import com.microsoft.appcenter.utils.AppCenterLog;

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
     * Gets the information about the existing update and returns it.
     *
     * @return information about the existing update.
     */
    public CodePushUpdateResponseUpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    /**
     * Sets the information about the existing update.
     *
     * @param updateInfo information about the existing update.
     */
    public void setUpdateInfo(CodePushUpdateResponseUpdateInfo updateInfo) {
        if (updateInfo != null) {
            this.updateInfo = updateInfo;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"updateInfo\" property cannot be null.");
        }
    }
}
