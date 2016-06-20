package com.microsoft.android.crash;

/**
 * <h3>Description</h3>
 *
 * Abstract class for callbacks to be invoked from the Crashes.
 *
 **/
public abstract class CrashesListener {
    /**
     * Return true to ignore the default exception handler, i.e. the user will not
     * get the alert dialog with the "Force Close" button.
     *
     * @return if true, the default handler is ignored
     */
    public boolean ignoreDefaultHandler() {
        return false;
    }

    /**
     * Return false to remove the device data (OS version, manufacturer, model)
     * from the crash log, e.g. if some of your testers are using unreleased
     * devices.
     *
     * @return if true, the crash report will include device data
     */
    public boolean includeDeviceData() {
        return true;
    }

    /**
     * Return false to remove the stable device identifier from the
     * crash log, e.g. if there are privacy concerns.
     *
     * @return if true, the crash report will include a stable device identifier
     */
    public boolean includeDeviceIdentifier() {
        return true;
    }

    /**
     * Return true to include information about the crashed thread if available.
     *
     * @return if true, the crash report will include thread id and name if available
     */
    public boolean includeThreadDetails() {
        return true;
    }

    /**
     * Return contact data or similar; note that this has privacy implications,
     * so you might want to return nil for release builds! The string will be
     * limited to 255 characters.
     *
     * @return the contact string
     */
    public String getContact() {
        return null;
    }

    /**
     * Return additional data, i.e. parts of the system log, the last server
     * response or similar. This string is not limited to a certain size.
     *
     * @return a description
     */
    public String getDescription() {
        return null;
    }

    /**
     * Return a user ID or similar; note that this has privacy implications,
     * so you might want to return nil for release builds! The string will be
     * limited to 255 characters.
     *
     * @return the user's ID
     */
    public String getUserID() {
        return null;
    }

    /**
     * Called when the crash manager found one or more crashes. Return true
     * if you want to auto-send crashes (i.e. not ask the user)
     *
     * @return if true, crashes are sent automatically
     * @deprecated Replace this method with onNewCrashesFound,
     * onConfirmedCrashesFound, and shouldAutoUploadCrashReport.
     */
    public boolean onCrashesFound() {
        return false;
    }

    /**
     * Return true if you want to auto-send crashes. Note that this method
     * is only called if new crashes were found.
     *
     * @return if true, crashes are sent automatically
     */
    public boolean shouldAutoUploadCrashes() {
        return false;
    }

    /**
     * Called when the crash manager has found new crash logs.
     */
    public void onNewCrashesFound() {
    }

    /**
     * Called when the crash manager has found crash logs that were already
     * confirmed by the user or should have been auto uploaded, but the upload
     * failed, e.g. in case of a network failure.
     */
    public void onConfirmedCrashesFound() {
    }

    /**
     * Called when the crash manager has sent crashes to AvalancheHub.
     */
    public void onCrashesSent() {
    }

    /**
     * Called when the crash manager failed to send crashes to AvalancheHub, e.g.
     * because the device has no network connections.
     */
    public void onCrashesNotSent() {
    }

    /**
     * Called when the user denied to send crashes to AvalancheHub.
     */
    public void onUserDeniedCrashes() {
    }

    /**
     * Get the number of max retry attempts to send crashes to AvalancheHub.
     * Infinite retries if this value is set to -1
     *
     * @return the max number of retry attempts
     */
    public int getMaxRetryAttempts() {
        return 1;
    }

    /**
     * Called when dialog should be displayed to inform the user about crash.
     *
     * @return if true, alert-view is handled by user
     */
    public boolean onHandleAlertView() {
        return false;
    }
}
