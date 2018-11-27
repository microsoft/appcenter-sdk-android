package com.microsoft.appcenter.http;

public interface ServiceCall {

    /**
     * Waits if necessary, and then retrieves its status.
     *
     * @return true if the call was finished.
     */
    boolean ensureFinished();

    /**
     * Cancel the call if possible.
     */
    void cancel();
}
