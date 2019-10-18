package com.microsoft.appcenter.sasquatch.util;

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

    private Context mContext;

    private Uri mFileAttachment;

    private String mTextAttachment;

    private AttachmentsUtils(Context context, String textAttachment) {
        mFileAttachment = null;
        mContext = context;
        mTextAttachment = textAttachment;
    }

    public AttachmentsUtils(Context context, String fileUriString, String textAttachment) {
        this(context, textAttachment);
        mFileAttachment = Uri.parse(fileUriString);
    }

    public AttachmentsUtils(Context context, Uri fileUriString, String textAttachment) {
        this(context, textAttachment);
        mFileAttachment = fileUriString;
    }

    public Iterable<ErrorAttachmentLog> getErrorAttachments() {
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

    public String getFileAttachmentMimeType() {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(mFileAttachment.getScheme())) {
            mimeType = mContext.getContentResolver().getType(mFileAttachment);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mFileAttachment.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
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

    public byte[] getFileAttachmentData() throws SecurityException {
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
}
