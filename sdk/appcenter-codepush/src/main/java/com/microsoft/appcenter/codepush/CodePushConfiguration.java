package com.microsoft.appcenter.codepush;

/**
 * Provides info regarding current app state and settings.
 */
public final class CodePushConfiguration {

    /**
     * Value of <code>versionName</code> parameter from <code>build.gradle</code>.
     */
    private String appVersion;

    /**
     * Android client unique id.
     */
    private String clientUniqueId;

    /**
     * CodePush deployment key.
     */
    private String deploymentKey;

    /**
     * CodePush acquisition server URL.
     */
    private String serverUrl;

    /**
     * Package hash of currently running CodePush update.
     * See {@link com.microsoft.appcenter.codepush.enums.CodePushUpdateState} for details.
     */
    private String packageHash;

    /**
     * Get the appVersion value.
     *
     * @return appVersion value
     */
    public String getAppVersion(){
        return this.appVersion;
    }

    /**
     * Get the clientUniqueId value
     *
     * @return the clientUniqueId value
     */
    public String getClientUniqueId() {
        return this.clientUniqueId;
    }

    /**
     * Get the deploymentKey value
     *
     * @return the deploymentKey value
     */
    public String getDeploymentKey() {
        return this.deploymentKey;
    }

    /**
     * Get the serverUrl value
     *
     * @return the serverUrl value
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * Get the packageHash value
     *
     * @return the packageHash value
     */
    public String getPackageHash() {
        return this.packageHash;
    }

    /**
     * Set the appVersion value.
     *
     * @param appVersion the appVersion value to set
     */
    @SuppressWarnings("WeakerAccess")
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Set the clientUniqueId value.
     *
     * @param clientUniqueId the clientUniqueId value to set
     */
    @SuppressWarnings("WeakerAccess")
    public void setClientUniqueId(String clientUniqueId) {
        this.clientUniqueId = clientUniqueId;
    }

    /**
     * Set the deploymentKey value.
     *
     * @param deploymentKey the deploymentKey value to set
     */
    @SuppressWarnings("WeakerAccess")
    public void setDeploymentKey(String deploymentKey) {
        this.deploymentKey = deploymentKey;
    }

    /**
     * Set the serverUrl value.
     *
     * @param serverUrl the serverUrl value to set
     */
    @SuppressWarnings("WeakerAccess")
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Set the packageHash value.
     *
     * @param packageHash the serverUrl value to set
     */
    @SuppressWarnings("WeakerAccess")
    public void setPackageHash(String packageHash) {
        this.packageHash = packageHash;
    }
}