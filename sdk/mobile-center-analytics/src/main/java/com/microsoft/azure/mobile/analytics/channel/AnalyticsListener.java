package com.microsoft.azure.mobile.analytics.channel;

import com.microsoft.azure.mobile.analytics.ingestion.models.EventLog;

public interface AnalyticsListener {

    /**
     * Called right before sending an event log. The callback can be invoked multiple times based on the number of event logs.
     *
     * @param eventLog The event log that will be sent.
     */
    void onBeforeSending(EventLog eventLog);

    /**
     * Called when sending an event log failed.
     * The report failed to send after the maximum retries so it will be discarded and won't be retried.
     *
     * @param eventLog The event log that failed to send.
     * @param e           An exception that caused failure.
     */
    void onSendingFailed(EventLog eventLog, Exception e);

    /**
     * Called when a event log is sent successfully.
     *
     * @param eventLog The event log that was sent successfully.
     */
    void onSendingSucceeded(EventLog eventLog);
}
