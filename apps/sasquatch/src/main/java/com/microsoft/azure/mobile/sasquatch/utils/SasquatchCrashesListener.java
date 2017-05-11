package com.microsoft.azure.mobile.sasquatch.utils;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.microsoft.azure.mobile.crashes.AbstractCrashesListener;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;

class SasquatchCrashesListener extends AbstractCrashesListener {

    private final AppCompatActivity activity;

    SasquatchCrashesListener(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean shouldAwaitUserConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder
                .setTitle(com.microsoft.azure.mobile.sasquatch.R.string.crash_confirmation_dialog_title)
                .setMessage(com.microsoft.azure.mobile.sasquatch.R.string.crash_confirmation_dialog_message)
                .setPositiveButton(com.microsoft.azure.mobile.sasquatch.R.string.crash_confirmation_dialog_send_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Crashes.notifyUserConfirmation(Crashes.SEND);
                    }
                })
                .setNegativeButton(com.microsoft.azure.mobile.sasquatch.R.string.crash_confirmation_dialog_not_send_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Crashes.notifyUserConfirmation(Crashes.DONT_SEND);
                    }
                })
                .setNeutralButton(com.microsoft.azure.mobile.sasquatch.R.string.crash_confirmation_dialog_always_send_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);
                    }
                });
        builder.create().show();
        return true;
    }

    @Override
    public void onBeforeSending(ErrorReport report) {
        Toast.makeText(activity, com.microsoft.azure.mobile.sasquatch.R.string.crash_before_sending, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSendingFailed(ErrorReport report, Exception e) {
        Toast.makeText(activity, com.microsoft.azure.mobile.sasquatch.R.string.crash_sent_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSendingSucceeded(ErrorReport report) {

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        String message = String.format("%s\nCrash ID: %s\nThrowable: %s", com.microsoft.azure.mobile.sasquatch.R.string.crash_sent_succeeded, report.getId(), report.getThrowable().toString());
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}
