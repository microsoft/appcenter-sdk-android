package com.microsoft.azure.mobile.crashes;

import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;

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
    @WorkerThread
    boolean shouldProcess(ErrorReport report);

    /**
     * Called to determine whether it should wait for user confirmation before sending crash reports.
     *
     * @return <code>true</code> if it requires to be confirmed by a user, otherwise <code>false</code>.
     * If this method returns <code>true</code>, {@link Crashes#notifyUserConfirmation(int)} must be called by yourself.
     */
    @UiThread
    boolean shouldAwaitUserConfirmation();

    /**
     * Called to get additional information to be send as separate ErrorAttachmentLog logs
     * Attachments are optional so this method can also return <code>null</code>.
     *
     * @param report The crash report for additional information.
     * @return {@link Iterable<ErrorAttachmentLog>} instances of ErrorAttachmentLog to be sent as separate logs.
     */
    @WorkerThread
    Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report);

    /**
     * Called right before sending a crash report. The callback can be invoked multiple times based on the number of crash reports.
     *
     * @param report The crash report that will be sent.
     */
    @UiThread
    void onBeforeSending(ErrorReport report);

    /**
     * Called when sending a crash report failed.
     * The report failed to send after the maximum retries so it will be discarded and won't be retried.
     *
     * @param report The crash report that failed to send.
     * @param e      An exception that caused failure.
     */
    @UiThread
    void onSendingFailed(ErrorReport report, Exception e);

    /**
     * Called when a crash report is sent successfully.
     *
     * @param report The crash report that was sent successfully.
     */
    @UiThread
    void onSendingSucceeded(ErrorReport report);
}
