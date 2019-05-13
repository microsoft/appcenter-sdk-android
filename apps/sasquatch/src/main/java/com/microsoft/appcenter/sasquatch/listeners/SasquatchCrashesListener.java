/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.listeners;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.activities.MainActivity;
import com.microsoft.appcenter.utils.HandlerUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class SasquatchCrashesListener extends AbstractCrashesListener {

    @VisibleForTesting
    public static final CountingIdlingResource crashesIdlingResource = new CountingIdlingResource("crashes");

    private final Context mContext;

    private static final long TOAST_DELAY = 2000;

    private String mTextAttachment;

    private Uri mFileAttachment;

    private long mBeforeSendingToastTime;

    public SasquatchCrashesListener(Context context) {
        this.mContext = context;
    }

    public String getTextAttachment() {
        return mTextAttachment;
    }

    public void setTextAttachment(String textAttachment) {
        this.mTextAttachment = textAttachment;
    }

    public Uri getFileAttachment() {
        return mFileAttachment;
    }

    public void setFileAttachment(Uri fileAttachment) {
        this.mFileAttachment = fileAttachment;
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
        List<ErrorAttachmentLog> attachments = new LinkedList<>();

        /* Attach app icon to test binary. */
        if (mFileAttachment != null) {
            try {
                byte[] data = getFileAttachmentData();
                String name = getFileAttachmentDisplayName();
                String mime = getFileAttachmentMimeType();
                ErrorAttachmentLog binaryLog = ErrorAttachmentLog.attachmentWithBinary(data, name, mime);
                attachments.add(binaryLog);
            } catch (SecurityException e) {
                Log.e(LOG_TAG, "Couldn't get file attachment data.", e);

                /* Reset file attachment. */
                MainActivity.setFileAttachment(null);
            }
        }

        /* Attach some text. */
        if (!TextUtils.isEmpty(mTextAttachment)) {
            ErrorAttachmentLog textLog = ErrorAttachmentLog.attachmentWithText(mTextAttachment, "text.txt");
            attachments.add(textLog);
        }

        /* Return attachments as list. */
        return attachments.size() > 0 ? attachments : null;
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
        if (report.getThrowable() != null) {
            message += String.format("\nThrowable: %s", report.getThrowable().toString());
        }
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

    public String getFileAttachmentDisplayName() throws SecurityException {
        Cursor cursor = mContext.getContentResolver()
                .query(mFileAttachment, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (!cursor.isNull(nameIndex)) {
                    return cursor.getString(nameIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    public String getFileAttachmentSize() throws SecurityException {
        Cursor cursor = mContext.getContentResolver()
                .query(mFileAttachment, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    return Formatter.formatFileSize(mContext, cursor.getLong(sizeIndex));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "Unknown";
    }

    private byte[] getFileAttachmentData() throws SecurityException {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = mContext.getContentResolver().openInputStream(mFileAttachment);
            if (inputStream == null) {
                return null;
            }
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't read file", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ignore) {
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ignore) {
            }
        }
        return outputStream != null ? outputStream.toByteArray() : null;
    }

    private String getFileAttachmentMimeType() {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(mFileAttachment.getScheme())) {
            mimeType = mContext.getContentResolver().getType(mFileAttachment);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mFileAttachment.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }
}
