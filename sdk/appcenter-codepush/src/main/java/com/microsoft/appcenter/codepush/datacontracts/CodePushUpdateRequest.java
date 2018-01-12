package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * A request class for querying for updates.
 */
public class CodePushUpdateRequest {

    /**
     * Specifies the deployment key you want to query for an update against.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * Specifies the current app version.
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * Specifies the current local package hash.
     */
    @SerializedName("packageHash")
    private String packageHash;

    /**
     * Whether to ignore the application version.
     */
    @SerializedName("isCompanion")
    private boolean isCompanion;

    /**
     * Specifies the current package label.
     */
    @SerializedName("label")
    private String label;

    /**
     * Device id.
     */
    @SerializedName("clientUniqueId")
    private String clientUniqueId;

    /**
     * Creates an update request based on the current {@link CodePushLocalPackage}.
     *
     * @param deploymentKey   the deployment key you want to query for an update against.
     * @param codePushPackage currently installed local package.
     * @param clientUniqueId  id of the device.
     * @return instance of the {@link CodePushUpdateRequest}.
     */
    public static CodePushUpdateRequest createUpdateRequest(final String deploymentKey, final CodePushLocalPackage codePushPackage, final String clientUniqueId) {
        CodePushUpdateRequest codePushUpdateRequest = new CodePushUpdateRequest();
        codePushUpdateRequest.setAppVersion(codePushPackage.getAppVersion());
        codePushUpdateRequest.setDeploymentKey(deploymentKey);
        codePushUpdateRequest.setPackageHash(codePushPackage.getPackageHash());
        codePushUpdateRequest.setLabel(codePushPackage.getLabel());
        codePushUpdateRequest.setClientUniqueId(clientUniqueId);
        return codePushUpdateRequest;
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
     * Gets the value of isCompanion and returns it.
     *
     * @return isCompanion.
     */
    public boolean isCompanion() {
        return isCompanion;
    }

    /**
     * Sets the isCompanion.
     *
     * @param companion new value.
     */
    public void setCompanion(boolean companion) {
        isCompanion = companion;
    }

    /**
     * Gets the value of label and returns it.
     *
     * @return label.
     */
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
     * Gets the value of clientUniqueId and returns it.
     *
     * @return clientUniqueId.
     */
    public String getClientUniqueId() {
        return clientUniqueId;
    }

    /**
     * Sets the clientUniqueId.
     *
     * @param clientUniqueId new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setClientUniqueId(String clientUniqueId) {
        this.clientUniqueId = clientUniqueId;
    }
}
