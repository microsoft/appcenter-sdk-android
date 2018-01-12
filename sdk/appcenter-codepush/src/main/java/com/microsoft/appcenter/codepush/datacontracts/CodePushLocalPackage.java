package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Represents the downloaded package.
 */
public class CodePushLocalPackage extends CodePushPackage {

    /**
     * Indicates whether this update is in a "pending" state.
     * When <code>true</code>, that means the update has been downloaded and installed, but the app restart
     * needed to apply it hasn't occurred yet, and therefore, its changes aren't currently visible to the end-user.
     */
    @SerializedName("isPending")
    private boolean isPending;

    /**
     * Indicates whether this is the first time the update has been run after being installed.
     */
    @SerializedName("isFirstRun")
    private boolean isFirstRun;

    /**
     * Whether this package is intended for debug mode.
     */
    @SerializedName("_isDebugOnly")
    private boolean isDebugOnly;

    /**
     * An exception that has occurred during downloading (can be null if package has been successfully downloaded).
     */
    @Expose
    private Exception downloadException;

    /**
     * Creates an instance of the package which has had an error during download.
     *
     * @param downloadException exception that has occurred.
     * @return instance of the {@link CodePushLocalPackage}.
     */
    public static CodePushLocalPackage createFailedLocalPackage(Exception downloadException) {
        CodePushLocalPackage codePushLocalPackage = new CodePushLocalPackage();
        codePushLocalPackage.setDownloadException(downloadException);
        return codePushLocalPackage;
    }

    /**
     * Creates an instance of the package from basic package.
     *
     * @param failedInstall   whether this update has been previously installed but was rolled back.
     * @param isFirstRun      whether this is the first time the update has been run after being installed.
     * @param isPending       whether this update is in a "pending" state.
     * @param isDebugOnly     whether this package is intended for debug mode.
     * @param codePushPackage basic package containing the information.
     * @return instance of the {@link CodePushLocalPackage}.
     */
    public static CodePushLocalPackage createLocalPackage(final boolean failedInstall, final boolean isFirstRun, final boolean isPending, final boolean isDebugOnly, final CodePushPackage codePushPackage) {
        CodePushLocalPackage codePushLocalPackage = new CodePushLocalPackage();
        codePushLocalPackage.setAppVersion(codePushPackage.getAppVersion());
        codePushLocalPackage.setDeploymentKey(codePushPackage.getDeploymentKey());
        codePushLocalPackage.setDescription(codePushPackage.getDescription());
        codePushLocalPackage.setFailedInstall(failedInstall);
        codePushLocalPackage.setMandatory(codePushPackage.isMandatory());
        codePushLocalPackage.setLabel(codePushPackage.getLabel());
        codePushLocalPackage.setPackageHash(codePushPackage.getPackageHash());
        codePushLocalPackage.setPending(isPending);
        codePushLocalPackage.setFirstRun(isFirstRun);
        codePushLocalPackage.setDebugOnly(isDebugOnly);
        return codePushLocalPackage;
    }

    /**
     * Gets the value of IsPending and returns it.
     *
     * @return isPending.
     */
    public boolean isPending() {
        return isPending;
    }

    /**
     * Sets the IsPending.
     *
     * @param pending new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setPending(boolean pending) {
        isPending = pending;
    }

    /**
     * Gets the value of isFirstRun and returns it.
     *
     * @return isFirstRun.
     */
    public boolean isFirstRun() {
        return isFirstRun;
    }

    /**
     * Sets the isFirstRun.
     *
     * @param firstRun new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setFirstRun(boolean firstRun) {
        isFirstRun = firstRun;
    }

    /**
     * Gets the value of isDebugOnly and returns it.
     *
     * @return isDebugOnly.
     */
    public boolean isDebugOnly() {
        return isDebugOnly;
    }

    /**
     * Sets the isDebugOnly.
     *
     * @param debugOnly new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDebugOnly(boolean debugOnly) {
        isDebugOnly = debugOnly;
    }

    /**
     * Gets the value of downloadException and returns it.
     *
     * @return downloadException.
     */
    public Exception getDownloadException() {
        return downloadException;
    }

    /**
     * Sets the downloadException.
     *
     * @param downloadException new value.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDownloadException(Exception downloadException) {
        this.downloadException = downloadException;
    }
}
