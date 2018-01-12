package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Basic package class. Contains all the basic information about the update package.
 * Extended by {@link CodePushRemotePackage} and {@link CodePushLocalPackage}.
 */
public class CodePushPackage {

    /**
     * The app binary version that this update is dependent on. This is the value that was
     * specified via the appStoreVersion parameter when calling the CLI's release command.
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * The deployment key that was used to originally download this update.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * The description of the update. This is the same value that you specified in the CLI when you released the update.
     */
    @SerializedName("description")
    private String description;

    /**
     * Indicates whether this update has been previously installed but was rolled back.
     */
    @SerializedName("failedInstall")
    private boolean failedInstall;

    /**
     * Indicates whether the update is considered mandatory.
     * This is the value that was specified in the CLI when the update was released.
     */
    @SerializedName("isMandatory")
    private boolean isMandatory;

    /**
     * The internal label automatically given to the update by the CodePush server.
     * This value uniquely identifies the update within its deployment.
     */
    @SerializedName("label")
    private String label;

    /**
     * The SHA hash value of the update.
     */
    @SerializedName("packageHash")
    private String packageHash;

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
     * Gets the value of deploymentKey and returns it.
     *
     * @return deploymentKey.
     */
    public String getDeploymentKey() {
        return deploymentKey;
    }

    /**
     * Sets the deploymentKey.
     *
     * @param deploymentKey new value.
     */
    public void setDeploymentKey(String deploymentKey) {
        this.deploymentKey = deploymentKey;
    }

    /**
     * Gets the value of description and returns it.
     *
     * @return description.
     */
    @SuppressWarnings("WeakerAccess")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the value of failedInstall and returns it.
     *
     * @return failedInstall.
     */
    public boolean isFailedInstall() {
        return failedInstall;
    }

    /**
     * Sets the failedInstall.
     *
     * @param failedInstall new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setFailedInstall(boolean failedInstall) {
        this.failedInstall = failedInstall;
    }

    /**
     * Gets the value of isMandatory and returns it.
     *
     * @return isMandatory.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * Sets the isMandatory.
     *
     * @param mandatory new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    /**
     * Gets the value of label and returns it.
     *
     * @return label.
     */
    @SuppressWarnings("WeakerAccess")
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label.
     *
     * @param label new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the value of packageHash and returns it.
     *
     * @return packageHash.
     */
    public String getPackageHash() {
        return packageHash;
    }

    /**
     * Sets the packageHash.
     *
     * @param packageHash new value.
     */
    public void setPackageHash(String packageHash) {
        this.packageHash = packageHash;
    }
}
