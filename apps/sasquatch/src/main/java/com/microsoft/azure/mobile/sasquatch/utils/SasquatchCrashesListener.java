package com.microsoft.azure.mobile.sasquatch.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.microsoft.azure.mobile.crashes.AbstractCrashesListener;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.sasquatch.R;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class SasquatchCrashesListener extends AbstractCrashesListener {

    private final Activity activity;

    public SasquatchCrashesListener(Activity activity) {
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
    public Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report) {

        /* Attach some text. */
        ErrorAttachmentLog textLog = ErrorAttachmentLog.attachmentWithText("This is a text attachment.", "text.txt");

        /* Attach app icon to test binary. */
        Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bitMapData = stream.toByteArray();
        ErrorAttachmentLog binaryLog = ErrorAttachmentLog.attachmentWithBinary(bitMapData, "icon.jpeg", "image/jpeg");

        /* Return attachments as list. */
        return Arrays.asList(textLog, binaryLog);
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
