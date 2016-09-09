package com.microsoft.sonoma.crashes;

import com.microsoft.sonoma.crashes.model.ErrorAttachment;
import com.microsoft.sonoma.crashes.model.ErrorReport;

/**
 * Interface for error reporting listener.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "SameReturnValue", "UnusedParameters"})
public interface CrashesListener {

    /**
     * Called to determine whether an error report should be processed or not.
     *
     * @param crashReport An error report that will be sent.
     * @return <code>true</code> if it should be processed and sent, otherwise <code>false</code>.
     */
    boolean shouldProcess(ErrorReport crashReport);

    /**
     * Called to determine whether it should wait for user confirmation before sending error reports.
     *
     * @return <code>true</code> if it requires to be confirmed by a user, otherwise <code>false</code>.
     *         If this method returns <code>true</code>, {@link Crashes#notifyUserConfirmation(int)} must be called by yourself.
     */
    boolean shouldAwaitUserConfirmation();

    /**
     * Called to get additional information to be attached to a crash report before sending.
     * Attachment is an optional so this method can also return <code>null</code>.
     *
     * @param crashReport A crash report for additional information.
     * @return {@link ErrorAttachment} instance to be attached to the error report.
     */
    ErrorAttachment getErrorAttachment(ErrorReport crashReport);

    /**
     * Called right before sending crash reports. The callback can be invoked multiple times based on the number of crash reports.
     *
     * @param crashReport An error report that will be sent.
     */
    void onBeforeSending(ErrorReport crashReport);

    /**
     * Called when it failed to send a crash report.
     * It failed after the maximum retries so the crash report is discarded and won't be retried.
     *
     * @param crashReport An error report that failed to send.
     * @param e           An exception that caused failure.
     */
    void onSendingFailed(ErrorReport crashReport, Exception e);

    /**
     * Called when it was sent successfully.
     *
     * @param crashReport The crash report that was sent.
     */
    void onSendingSucceeded(ErrorReport crashReport);
}
