package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Update info from the server.
 */
public class CodePushUpdateResponseUpdateInfo {

    /**
     * Url to access package on server.
     */
    @SerializedName("downloadURL")
    private String downloadUrl;

    /**
     * The description of the update.
     * This is the same value that you specified in the CLI when you released the update.
     */
    @SerializedName("description")
    private String description;

    /**
     * Whether the package is available (<code>false</code> if it it disabled).
     */
    @SerializedName("isAvailable")
    private boolean isAvailable;

    /**
     * Indicates whether the update is considered mandatory.
     * This is the value that was specified in the CLI when the update was released.
     */
    @SerializedName("isMandatory")
    private boolean isMandatory;

    /**
     * The app binary version that this update is dependent on. This is the value that was
     * specified via the appStoreVersion parameter when calling the CLI's release command.
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * The SHA hash value of the update.
     */
    @SerializedName("packageHash")
    private String packageHash;

    /**
     * The internal label automatically given to the update by the CodePush server.
     * This value uniquely identifies the update within its deployment.
     */
    @SerializedName("label")
    private String label;

    /**
     * Size of the package.
     */
    @SerializedName("packageSize")
    private long packageSize;

    /**
     * Whether the client should trigger a store update.
     */
    @SerializedName("updateAppVersion")
    private boolean updateAppVersion;

    /**
     * Set to <code>true</code> if the update directs to use the binary version of the application.
     */
    @SerializedName("shouldRunBinaryVersion")
    private boolean shouldRunBinaryVersion;

    /**
     * Gets the value of downloadUrl and returns it.
     *
     * @return downloadUrl.
     */
    @SuppressWarnings("WeakerAccess")
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets the downloadUrl.
     *
     * @param downloadUrl new value.
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
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
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the value of isAvailable and returns it.
     *
     * @return isAvailable.
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Sets the isAvailable.
     *
     * @param available new value.
     */
    public void setAvailable(boolean available) {
        isAvailable = available;
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
    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

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
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the value of packageSize and returns it.
     *
     * @return packageSize.
     */
    @SuppressWarnings("WeakerAccess")
    public long getPackageSize() {
        return packageSize;
    }

    /**
     * Sets the packageSize.
     *
     * @param packageSize new value.
     */
    public void setPackageSize(long packageSize) {
        this.packageSize = packageSize;
    }

    /**
     * Gets the value of updateAppVersion and returns it.
     *
     * @return updateAppVersion.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isUpdateAppVersion() {
        return updateAppVersion;
    }

    /**
     * Sets the updateAppVersion.
     *
     * @param updateAppVersion new value.
     */
    public void setUpdateAppVersion(boolean updateAppVersion) {
        this.updateAppVersion = updateAppVersion;
    }

    /**
     * Gets the value of shouldRunBinaryVersion and returns it.
     *
     * @return shouldRunBinaryVersion.
     */
    public boolean isShouldRunBinaryVersion() {
        return shouldRunBinaryVersion;
    }

    /**
     * Sets the shouldRunBinaryVersion.
     *
     * @param shouldRunBinaryVersion new value.
     */
    public void setShouldRunBinaryVersion(boolean shouldRunBinaryVersion) {
        this.shouldRunBinaryVersion = shouldRunBinaryVersion;
    }
}