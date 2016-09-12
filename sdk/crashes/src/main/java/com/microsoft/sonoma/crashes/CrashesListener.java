package com.microsoft.sonoma.crashes;

import com.microsoft.sonoma.crashes.model.ErrorAttachment;
import com.microsoft.sonoma.crashes.model.ErrorReport;

/**
 * Interface for the crashes listener.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "SameReturnValue", "UnusedParameters"})
public interface CrashesListener {

    /**
     * Called to determine whether a crash report should be processed or not.
     *
     * @param report A crash report that will be sent.
     * @return <code>true</code> if it should be processed and sent, otherwise <code>false</code>.
     */
    boolean shouldProcess(ErrorReport report);

    /**
     * Called to determine whether it should wait for user confirmation before sending crash reports.
     *
     * @return <code>true</code> if it requires to be confirmed by a user, otherwise <code>false</code>.
     *         If this method returns <code>true</code>, {@link Crashes#notifyUserConfirmation(int)} must be called by yourself.
     */
    boolean shouldAwaitUserConfirmation();

    /**
     * Called to get additional information to be attached to a crash report before sending.
     * Attachment is an optional so this method can also return <code>null</code>.
     *
     * @param report The crash report for additional information.
     * @return {@link ErrorAttachment} instance to be attached to the crash report.
     */
    ErrorAttachment getErrorAttachment(ErrorReport report);

    /**
     * Called right before sending a crash report. The callback can be invoked multiple times based on the number of crash reports.
     *
     * @param report The crash report that will be sent.
     */
    void onBeforeSending(ErrorReport report);

    /**
     * Called when sending a crash report failed.
     * The report failed to send after the maximum retries so it will be discarded and won't be retried.
     *
     * @param report The crash report that failed to send.
     * @param e           An exception that caused failure.
     */
    void onSendingFailed(ErrorReport report, Exception e);

    /**
     * Called when a crash report was sent successfully.
     *
     * @param report The crash report that was sent.
     */
    void onSendingSucceeded(ErrorReport report);
}
