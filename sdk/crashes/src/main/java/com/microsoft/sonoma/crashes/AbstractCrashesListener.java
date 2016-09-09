package com.microsoft.sonoma.crashes;

import com.microsoft.sonoma.crashes.model.ErrorAttachment;
import com.microsoft.sonoma.crashes.model.ErrorReport;

/**
 * Abstract class with default behaviors for error reporting listener.
 */
public abstract class AbstractCrashesListener implements CrashesListener {
    @Override
    public boolean shouldProcess(ErrorReport crashReport) {
        return true;
    }

    @Override
    public boolean shouldAwaitUserConfirmation() {
        return false;
    }

    @Override
    public ErrorAttachment getErrorAttachment(ErrorReport crashReport) {
        return null;
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onBeforeSending(ErrorReport crashReport) {
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onSendingFailed(ErrorReport crashReport, Exception e) {
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onSendingSucceeded(ErrorReport crashReport) {
    }
}
