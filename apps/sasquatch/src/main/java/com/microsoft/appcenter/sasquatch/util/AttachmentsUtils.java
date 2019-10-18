package com.microsoft.appcenter.sasquatch.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.sasquatch.activities.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class AttachmentsUtils {

    private Uri mFileAttachment;

    private String mTextAttachment;

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

    public void setFileAttachment(String fileAttachment) {
        this.mFileAttachment = Uri.parse(fileAttachment);
    }

    @SuppressLint("StaticFieldLeak")
    private static AttachmentsUtils instance;

    public static AttachmentsUtils getInstance() {
        if(instance == null) {
            instance = new AttachmentsUtils();
        }
        return instance;
    }

    private AttachmentsUtils() {
    }

    public Iterable<ErrorAttachmentLog> getErrorAttachments(Context context) {
        List<ErrorAttachmentLog> attachments = new LinkedList<>();

        /* Attach app icon to test binary. */
        if (mFileAttachment != null) {
            try {
                byte[] data = getFileAttachmentData(context);
                String name = getFileAttachmentDisplayName(context);
                String mime = getFileAttachmentMimeType(context);
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

    public String getFileAttachmentMimeType(Context context) {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(mFileAttachment.getScheme())) {
            mimeType = context.getContentResolver().getType(mFileAttachment);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mFileAttachment.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }

    public String getFileAttachmentDisplayName(Context context) throws SecurityException {
        Cursor cursor = context.getContentResolver()
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

    public byte[] getFileAttachmentData(Context context) throws SecurityException {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(mFileAttachment);
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

    public String getFileAttachmentSize(Context context) throws SecurityException {
        Cursor cursor = context.getContentResolver()
                .query(mFileAttachment, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    return Formatter.formatFileSize(context, cursor.getLong(sizeIndex));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "Unknown";
    }
}
