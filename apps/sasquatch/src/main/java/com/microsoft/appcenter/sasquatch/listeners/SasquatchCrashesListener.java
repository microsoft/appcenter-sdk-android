package com.microsoft.appcenter.sasquatch.listeners;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.sasquatch.R;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class SasquatchCrashesListener extends AbstractCrashesListener {

    private Context context;

    @VisibleForTesting
    public static final CountingIdlingResource crashesIdlingResource = new CountingIdlingResource("crashes");

    public SasquatchCrashesListener(Context context) {
        this.context = context;
    }

    @Override
    public boolean shouldAwaitUserConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle(R.string.crash_confirmation_dialog_title)
                .setMessage(R.string.crash_confirmation_dialog_message)
                .setPositiveButton(R.string.crash_confirmation_dialog_send_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Crashes.notifyUserConfirmation(Crashes.SEND);
                    }
                })
                .setNegativeButton(R.string.crash_confirmation_dialog_not_send_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Crashes.notifyUserConfirmation(Crashes.DONT_SEND);
                    }
                })
                .setNeutralButton(R.string.crash_confirmation_dialog_always_send_button, new DialogInterface.OnClickListener() {
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
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bitMapData = stream.toByteArray();
        ErrorAttachmentLog binaryLog = ErrorAttachmentLog.attachmentWithBinary(bitMapData, "icon.jpeg", "image/jpeg");

                /* Return attachments as list. */
        return Arrays.asList(textLog, binaryLog);
    }

    @Override
    public void onBeforeSending(ErrorReport report) {
        Toast.makeText(context, R.string.crash_before_sending, Toast.LENGTH_SHORT).show();
        crashesIdlingResource.increment();
    }

    @Override
    public void onSendingFailed(ErrorReport report, Exception e) {
        Toast.makeText(context, R.string.crash_sent_failed, Toast.LENGTH_SHORT).show();
        crashesIdlingResource.decrement();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void onSendingSucceeded(ErrorReport report) {
        String message = String.format("%s\nCrash ID: %s", context.getString(R.string.crash_sent_succeeded), report.getId());
        if (report.getThrowable() != null) {
            message += String.format("\nThrowable: %s", report.getThrowable().toString());
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        crashesIdlingResource.decrement();
    }
}
