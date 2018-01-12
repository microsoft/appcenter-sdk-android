package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * An "options" object used to determine whether a confirmation dialog should be displayed to the end user when an update is available,
 * and if so, what strings to use. Defaults to null, which has the effect of disabling the dialog completely. Setting this to any truthy
 * value will enable the dialog with the default strings, and passing an object to this parameter allows enabling the dialog as well as
 * overriding one or more of the default strings.
 */
public class CodePushUpdateDialog {

    /**
     * Indicates whether you would like to append the description of an available release to the
     * notification message which is displayed to the end user.
     * Defaults to <code>false</code>.
     */
    @SerializedName("appendReleaseDescription")
    private boolean appendReleaseDescription;

    /**
     * Indicates the string you would like to prefix the release description with, if any, when
     * displaying the update notification to the end user.
     * Defaults to " Description: ".
     */
    @SerializedName("descriptionPrefix")
    private String descriptionPrefix;

    /**
     * The text to use for the button the end user must press in order to install a mandatory update.
     * Defaults to "Continue".
     */
    @SerializedName("mandatoryContinueButtonLabel")
    private String mandatoryContinueButtonLabel;

    /**
     * The text used as the body of an update notification, when the update is specified as mandatory.
     * Defaults to "An update is available that must be installed.".
     */
    @SerializedName("mandatoryUpdateMessage")
    private String mandatoryUpdateMessage;

    /**
     * The text to use for the button the end user can press in order to ignore an optional update that is available.
     * Defaults to "Ignore".
     */
    @SerializedName("optionalIgnoreButtonLabel")
    private String optionalIgnoreButtonLabel;

    /**
     * The text to use for the button the end user can press in order to install an optional update.
     * Defaults to "Install".
     */
    @SerializedName("optionalInstallButtonLabel")
    private String optionalInstallButtonLabel;

    /**
     * The text used as the body of an update notification, when the update is optional.
     * Defaults to "An update is available. Would you like to install it?".
     */
    @SerializedName("optionalUpdateMessage")
    private String optionalUpdateMessage;

    /**
     * The text used as the header of an update notification that is displayed to the end user.
     * Defaults to "Update available".
     */
    @SerializedName("title")
    public String title;

    /**
     * Creates default dialog with default button labels and messages.
     *
     * @return instance of the {@link CodePushUpdateDialog}.
     */
    public static CodePushUpdateDialog getDefaultDialog() {
        CodePushUpdateDialog codePushUpdateDialog = new CodePushUpdateDialog();
        codePushUpdateDialog.setDescriptionPrefix("Description: ");
        codePushUpdateDialog.setMandatoryContinueButtonLabel("Continue");
        codePushUpdateDialog.setMandatoryUpdateMessage("An update is available that must be installed.");
        codePushUpdateDialog.setOptionalIgnoreButtonLabel("Ignore");
        codePushUpdateDialog.setOptionalInstallButtonLabel("Install");
        codePushUpdateDialog.setOptionalUpdateMessage("An update is available. Would you like to install it?");
        codePushUpdateDialog.setTitle("Update available");
        return codePushUpdateDialog;
    }

    /**
     * Gets the value of appendReleaseDescription and returns it.
     *
     * @return appendReleaseDescription.
     */
    public boolean getAppendReleaseDescription() {
        return appendReleaseDescription;
    }

    /**
     * Sets the appendReleaseDescription.
     *
     * @param appendReleaseDescription new value.
     */
    public void setAppendReleaseDescription(boolean appendReleaseDescription) {
        this.appendReleaseDescription = appendReleaseDescription;
    }

    /**
     * Gets the value of descriptionPrefix and returns it.
     *
     * @return descriptionPrefix.
     */
    public String getDescriptionPrefix() {
        return descriptionPrefix;
    }

    /**
     * Sets the descriptionPrefix.
     *
     * @param descriptionPrefix new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDescriptionPrefix(String descriptionPrefix) {
        this.descriptionPrefix = descriptionPrefix;
    }

    /**
     * Gets the value of mandatoryContinueButtonLabel and returns it.
     *
     * @return mandatoryContinueButtonLabel.
     */
    public String getMandatoryContinueButtonLabel() {
        return mandatoryContinueButtonLabel;
    }

    /**
     * Sets the mandatoryContinueButtonLabel.
     *
     * @param mandatoryContinueButtonLabel new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatoryContinueButtonLabel(String mandatoryContinueButtonLabel) {
        this.mandatoryContinueButtonLabel = mandatoryContinueButtonLabel;
    }

    /**
     * Gets the value of mandatoryUpdateMessage and returns it.
     *
     * @return mandatoryUpdateMessage.
     */
    public String getMandatoryUpdateMessage() {
        return mandatoryUpdateMessage;
    }

    /**
     * Sets the mandatoryUpdateMessage.
     *
     * @param mandatoryUpdateMessage new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setMandatoryUpdateMessage(String mandatoryUpdateMessage) {
        this.mandatoryUpdateMessage = mandatoryUpdateMessage;
    }

    /**
     * Gets the value of optionalIgnoreButtonLabel and returns it.
     *
     * @return optionalIgnoreButtonLabel.
     */
    public String getOptionalIgnoreButtonLabel() {
        return optionalIgnoreButtonLabel;
    }

    /**
     * Sets the optionalIgnoreButtonLabel.
     *
     * @param optionalIgnoreButtonLabel new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setOptionalIgnoreButtonLabel(String optionalIgnoreButtonLabel) {
        this.optionalIgnoreButtonLabel = optionalIgnoreButtonLabel;
    }

    /**
     * Gets the value of optionalInstallButtonLabel and returns it.
     *
     * @return optionalInstallButtonLabel.
     */
    public String getOptionalInstallButtonLabel() {
        return optionalInstallButtonLabel;
    }

    /**
     * Sets the optionalInstallButtonLabel.
     *
     * @param optionalInstallButtonLabel new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setOptionalInstallButtonLabel(String optionalInstallButtonLabel) {
        this.optionalInstallButtonLabel = optionalInstallButtonLabel;
    }

    /**
     * Gets the value of optionalUpdateMessage and returns it.
     *
     * @return optionalUpdateMessage.
     */
    public String getOptionalUpdateMessage() {
        return optionalUpdateMessage;
    }

    /**
     * Sets the optionalUpdateMessage.
     *
     * @param optionalUpdateMessage new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setOptionalUpdateMessage(String optionalUpdateMessage) {
        this.optionalUpdateMessage = optionalUpdateMessage;
    }

    /**
     * Gets the value of title and returns it.
     *
     * @return title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     *
     * @param title new value.
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
