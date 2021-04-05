/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.listeners;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.SystemClock;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.test.espresso.idling.CountingIdlingResource;
import android.widget.Toast;

import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.util.AttachmentsUtil;
import com.microsoft.appcenter.utils.HandlerUtils;

public class SasquatchCrashesListener extends AbstractCrashesListener {

    @VisibleForTesting
    public static final CountingIdlingResource crashesIdlingResource = new CountingIdlingResource("crashes");

    private final Context mContext;

    private static final long TOAST_DELAY = 2000;

    private long mBeforeSendingToastTime;

    public SasquatchCrashesListener(Context context) {
        this.mContext = context;
    }

    public String getTextAttachment() {
        return AttachmentsUtil.getInstance().getTextAttachment();
    }

    public void setTextAttachment(String textAttachment) {
        AttachmentsUtil.getInstance().setTextAttachment(textAttachment);
    }

    public Uri getFileAttachment() {
        return AttachmentsUtil.getInstance().getFileAttachment();
    }

    public void setFileAttachment(Uri fileAttachment) {
        AttachmentsUtil.getInstance().setFileAttachment(fileAttachment);
    }

    @Override
    public boolean shouldAwaitUserConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
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

        /* Return attachments as list. */
        return AttachmentsUtil.getInstance().getErrorAttachments(mContext);
    }

    @Override
    public void onBeforeSending(ErrorReport report) {
        mBeforeSendingToastTime = SystemClock.uptimeMillis();
        Toast.makeText(mContext, R.string.crash_before_sending, Toast.LENGTH_SHORT).show();
        crashesIdlingResource.increment();
    }

    @Override
    public void onSendingFailed(ErrorReport report, Exception e) {
        notifySending(mContext.getString(R.string.crash_sent_failed));
    }

    @Override
    public void onSendingSucceeded(ErrorReport report) {
        String message = String.format("%s\nCrash ID: %s", mContext.getString(R.string.crash_sent_succeeded), report.getId());
        message += String.format("\nStackTrace: %s", report.getStackTrace());
        notifySending(message);
    }

    private void notifySending(final String message) {
        long timeToWait = mBeforeSendingToastTime + TOAST_DELAY - SystemClock.uptimeMillis();
        if (timeToWait <= 0) {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        } else {
            HandlerUtils.getMainHandler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            }, timeToWait);
        }
        crashesIdlingResource.decrement();
    }

    public String getFileAttachmentDisplayName() {
        return AttachmentsUtil.getInstance().getFileAttachmentDisplayName(mContext);
    }

    public String getFileAttachmentSize() throws SecurityException {
        return AttachmentsUtil.getInstance().getFileAttachmentSize(mContext);
    }
}
