package com.microsoft.sonoma.crashes;

import com.microsoft.sonoma.crashes.model.ErrorAttachment;
import com.microsoft.sonoma.crashes.model.ErrorReport;

/**
 * Abstract class with default behaviors for the crashes listener.
 */
public abstract class AbstractCrashesListener implements CrashesListener {
    @Override
    public boolean shouldProcess(ErrorReport report) {
        return true;
    }

    @Override
    public boolean shouldAwaitUserConfirmation() {
        return false;
    }

    @Override
    public ErrorAttachment getErrorAttachment(ErrorReport report) {
        return null;
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onBeforeSending(ErrorReport report) {
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onSendingFailed(ErrorReport report, Exception e) {
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onSendingSucceeded(ErrorReport report) {
    }
}
