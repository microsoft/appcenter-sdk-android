package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a report about the deployment.
 */
public class CodePushDeploymentStatusReport extends CodePushDownloadStatusReport {

    /**
     * The version of the app that was deployed (for a native app upgrade).
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * Deployment key used when deploying the previous package.
     */
    @SerializedName("previousDeploymentKey")
    private String previousDeploymentKey;

    /**
     * The label (v#) of the package that was upgraded from.
     */
    @SerializedName("previousLabelOrAppVersion")
    private String previousLabelOrAppVersion;

    /**
     * Whether the deployment succeeded or failed.
     */
    @SerializedName("status")
    private String status;

    /**
     * Gets the value of appVersion and returns it.
     *
     * @return appVersion.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Sets the appVersion.
     *
     * @param appVersion new value.
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Gets the value of previousDeploymentKey and returns it.
     *
     * @return previousDeploymentKey.
     */
    public String getPreviousDeploymentKey() {
        return previousDeploymentKey;
    }

    /**
     * Sets the previousDeploymentKey.
     *
     * @param previousDeploymentKey new value.
     */
    public void setPreviousDeploymentKey(String previousDeploymentKey) {
        this.previousDeploymentKey = previousDeploymentKey;
    }

    /**
     * Gets the value of previousLabelOrAppVersion and returns it.
     *
     * @return previousLabelOrAppVersion.
     */
    public String getPreviousLabelOrAppVersion() {
        return previousLabelOrAppVersion;
    }

    /**
     * Sets the previousLabelOrAppVersion.
     *
     * @param previousLabelOrAppVersion new value.
     */
    public void setPreviousLabelOrAppVersion(String previousLabelOrAppVersion) {
        this.previousLabelOrAppVersion = previousLabelOrAppVersion;
    }

    /**
     * Gets the value of status and returns it.
     *
     * @return status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status new value.
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
