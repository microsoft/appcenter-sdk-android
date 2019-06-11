/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes;

import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;

/**
 * Interface for the crashes listener.
 */
@SuppressWarnings({"UnusedReturnValue", "SameReturnValue", "UnusedParameters", "EmptyMethod", "WeakerAccess", "RedundantSuppression"})
public interface CrashesListener {

    /**
     * Called from a worker thread to determine whether a crash report should be processed or not.
     *
     * @param report A crash report that will be sent.
     * @return <code>true</code> if it should be processed and sent, otherwise <code>false</code>.
     */
    boolean shouldProcess(ErrorReport report);

    /**
     * Called from UI thread to determine whether it should wait for user confirmation before sending crash reports.
     *
     * @return <code>true</code> if it requires to be confirmed by a user, otherwise <code>false</code>.
     * If this method returns <code>true</code>, {@link Crashes#notifyUserConfirmation(int)} must be called by yourself.
     */
    @SuppressWarnings({"JavadocReference", "RedundantSuppression"})
    boolean shouldAwaitUserConfirmation();

    /**
     * Called from a worker thread to get additional information to be sent as separate ErrorAttachmentLog logs
     * Attachments are optional so this method can also return <code>null</code>.
     *
     * @param report The crash report for additional information.
     * @return instances of {@link ErrorAttachmentLog} to be sent for the specified error report.
     */
    Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report);

    /**
     * Called from UI thread right before sending a crash report. The callback can be invoked multiple times based on the number of crash reports.
     *
     * @param report The crash report that will be sent.
     */
    void onBeforeSending(ErrorReport report);

    /**
     * Called from UI thread  when sending a crash report failed.
     * The report failed to send after the maximum retries so it will be discarded and won't be retried.
     *
     * @param report The crash report that failed to send.
     * @param e      An exception that caused failure.
     */
    void onSendingFailed(ErrorReport report, Exception e);

    /**
     * Called from UI thread when a crash report is sent successfully.
     *
     * @param report The crash report that was sent successfully.
     */
    void onSendingSucceeded(ErrorReport report);
}
