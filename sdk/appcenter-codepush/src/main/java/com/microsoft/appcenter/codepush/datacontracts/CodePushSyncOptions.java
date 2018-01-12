package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.codepush.enums.CodePushCheckFrequency;
import com.microsoft.appcenter.codepush.enums.CodePushInstallMode;

/**
 * Contains synchronization options.
 */
public class CodePushSyncOptions {

    /**
     * Specifies the deployment key you want to query for an update against.
     * By default, this value is derived from the MainActivity.java file (Android),
     * but this option allows you to override it from the script-side if you need to
     * dynamically use a different deployment for a specific call to sync.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * Specifies when you would like to install optional updates (i.e. those that aren't marked as mandatory).
     * Defaults to {@link CodePushInstallMode#ON_NEXT_RESTART}.
     */
    @SerializedName("installMode")
    private CodePushInstallMode installMode;

    /**
     * Specifies when you would like to install updates which are marked as mandatory.
     * Defaults to {@link CodePushInstallMode#IMMEDIATE}.
     */
    @SerializedName("mandatoryInstallMode")
    private CodePushInstallMode mandatoryInstallMode;

    /**
     * Specifies the minimum number of seconds that the app needs to have been in the background before restarting the app.
     * This property only applies to updates which are installed using {@link CodePushInstallMode#ON_NEXT_RESUME},
     * and can be useful for getting your update in front of end users sooner, without being too obtrusive.
     * Defaults to `0`, which has the effect of applying the update immediately after a resume, regardless
     * how long it was in the background.
     */
    @SerializedName("minimumBackgroundDuration")
    private int minimumBackgroundDuration;

    /**
     * Specifies whether to ignore failed updates.
     * Defaults to <code>true</code>.
     */
    private boolean ignoreFailedUpdates;

    /**
     * An "options" object used to determine whether a confirmation dialog should be displayed to the end user when an update is available,
     * and if so, what strings to use. Defaults to null, which has the effect of disabling the dialog completely.
     * Setting this to any truthy value will enable the dialog with the default strings, and passing an object to this parameter allows
     * enabling the dialog as well as overriding one or more of the default strings.
     */
    @SerializedName("updateDialog")
    private CodePushUpdateDialog updateDialog;

    /**
     * Specifies when you would like to synchronize updates with the CodePush server.
     * Defaults to {@link CodePushCheckFrequency#ON_APP_START}.
     */
    @SerializedName("checkFrequency")
    private CodePushCheckFrequency checkFrequency;

    /**
     * Creates default instance of sync options.
     *
     * @param deploymentKey the deployment key you want to query for an update against.
     * @return instance of the {@link CodePushSyncOptions}.
     */
    public static CodePushSyncOptions getDefaultSyncOptions(String deploymentKey) {
        CodePushSyncOptions codePushSyncOptions = new CodePushSyncOptions();
        codePushSyncOptions.setDeploymentKey(deploymentKey);
        codePushSyncOptions.setInstallMode(CodePushInstallMode.ON_NEXT_RESTART);
        codePushSyncOptions.setMandatoryInstallMode(CodePushInstallMode.IMMEDIATE);
        codePushSyncOptions.setMinimumBackgroundDuration(0);
        codePushSyncOptions.setIgnoreFailedUpdates(true);
        codePushSyncOptions.setCheckFrequency(CodePushCheckFrequency.ON_APP_START);
        return codePushSyncOptions;
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
     * Gets the value of installMode and returns it.
     *
     * @return installMode.
     */
    public CodePushInstallMode getInstallMode() {
        return installMode;
    }

    /**
     * Sets the installMode.
     *
     * @param installMode new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setInstallMode(CodePushInstallMode installMode) {
        this.installMode = installMode;
    }

    /**
     * Gets the value of mandatoryInstallMode and returns it.
     *
     * @return mandatoryInstallMode.
     */
    public CodePushInstallMode getMandatoryInstallMode() {
        return mandatoryInstallMode;
    }

    /**
     * Sets the mandatoryInstallMode.
     *
     * @param mandatoryInstallMode new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatoryInstallMode(CodePushInstallMode mandatoryInstallMode) {
        this.mandatoryInstallMode = mandatoryInstallMode;
    }

    /**
     * Gets the value of minimumBackgroundDuration and returns it.
     *
     * @return minimumBackgroundDuration.
     */
    public int getMinimumBackgroundDuration() {
        return minimumBackgroundDuration;
    }

    /**
     * Sets the minimumBackgroundDuration.
     *
     * @param minimumBackgroundDuration new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMinimumBackgroundDuration(int minimumBackgroundDuration) {
        this.minimumBackgroundDuration = minimumBackgroundDuration;
    }

    /**
     * Gets the value of ignoreFailedUpdates and returns it.
     *
     * @return ignoreFailedUpdates.
     */
    public boolean getIgnoreFailedUpdates() {
        return ignoreFailedUpdates;
    }

    /**
     * Sets the ignoreFailedUpdates.
     *
     * @param ignoreFailedUpdates new value.
     */
    public void setIgnoreFailedUpdates(boolean ignoreFailedUpdates) {
        this.ignoreFailedUpdates = ignoreFailedUpdates;
    }

    /**
     * Gets the value of updateDialog and returns it.
     *
     * @return updateDialog.
     */
    public CodePushUpdateDialog getUpdateDialog() {
        return updateDialog;
    }

    /**
     * Sets the updateDialog.
     *
     * @param updateDialog new value.
     */
    public void setUpdateDialog(CodePushUpdateDialog updateDialog) {
        this.updateDialog = updateDialog;
    }

    /**
     * Gets the value of checkFrequency and returns it.
     *
     * @return checkFrequency.
     */
    public CodePushCheckFrequency getCheckFrequency() {
        return checkFrequency;
    }

    /**
     * Sets the checkFrequency.
     *
     * @param checkFrequency new value.
     */
    public void setCheckFrequency(CodePushCheckFrequency checkFrequency) {
        this.checkFrequency = checkFrequency;
    }
}
