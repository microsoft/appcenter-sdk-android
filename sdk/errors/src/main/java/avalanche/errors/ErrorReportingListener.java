package avalanche.errors;

import avalanche.errors.model.ErrorAttachment;
import avalanche.errors.model.ErrorReport;

/**
 * Interface for error reporting listener.
 */
public interface ErrorReportingListener {

    /**
     * Called to determine whether an error report should be processed or not.
     *
     * @param errorReport An error report that will be sent.
     * @return <code>true</code> if it should be processed and sent, otherwise <code>false</code>.
     */
    boolean shouldProcess(ErrorReport errorReport);

    /**
     * Called to determine whether it should wait for user confirmation before sending error reports.
     *
     * @return <code>true</code> if it requires to be confirmed by a user, otherwise <code>false</code>.
     *         If this method returns <code>true</code>, {@link ErrorReporting#notifyUserConfirmation(int)} must be called by yourself.
     */
    boolean shouldAwaitUserConfirmation();

    /**
     * Called to get additional information to be attached to an error report before sending.
     * Attachment is an optional so this method can also return <code>null</code>.
     *
     * @param errorReport An error report for additional information.
     * @return {@link ErrorAttachment} instance to be attached to the error report.
     */
    ErrorAttachment getErrorAttachment(ErrorReport errorReport);

    /**
     * Called right before sending error reports. The callback can be invoked multiple times based on the number of error reports.
     *
     * @param errorReport An error report that will be sent.
     */
    void onBeforeSending(ErrorReport errorReport);

    /**
     * Called when it failed to send an error report.
     * It failed after the maximum retries so the error report is discarded and won't be retried.
     *
     * @param errorReport An error report that failed to send.
     * @param e           An exception that caused failure.
     */
    void onSendingFailed(ErrorReport errorReport, Exception e);

    /**
     * Called when it was sent successfully.
     *
     * @param errorReport An error report that was sent.
     */
    void onSendingSucceeded(ErrorReport errorReport);
}
