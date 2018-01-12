package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a report sent after downloading package.
 */
public class CodePushDownloadStatusReport {

    /**
     * The id of the device.
     */
    @SerializedName("clientUniqueId")
    private String clientUniqueId;

    /**
     * The deployment key to use to query the CodePush server for an update.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * The internal label automatically given to the update by the CodePush server.
     * This value uniquely identifies the update within its deployment.
     */
    @SerializedName("label")
    private String label;

    /**
     * Creates a report using the provided information.
     *
     * @param clientUniqueId id of the device.
     * @param deploymentKey  deployment key to use to query the CodePush server for an update.
     * @param label          internal label automatically given to the update by the CodePush server.
     * @return instance of {@link CodePushDownloadStatusReport}.
     */
    public static CodePushDownloadStatusReport createReport(final String clientUniqueId, final String deploymentKey, final String label) {
        CodePushDownloadStatusReport codePushDownloadStatusReport = new CodePushDownloadStatusReport();
        codePushDownloadStatusReport.setClientUniqueId(clientUniqueId);
        codePushDownloadStatusReport.setDeploymentKey(deploymentKey);
        codePushDownloadStatusReport.setLabel(label);
        return codePushDownloadStatusReport;
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
     * Gets the value of deploymentKey and returns it.
     *
     * @return deploymentKey.
     */
    public String getDeploymentKey() {
        return deploymentKey;
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
    public void setLabel(String label) {
        this.label = label;
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

    /**
     * Sets the deploymentKey.
     *
     * @param deploymentKey new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDeploymentKey(String deploymentKey) {
        this.deploymentKey = deploymentKey;
    }
}
