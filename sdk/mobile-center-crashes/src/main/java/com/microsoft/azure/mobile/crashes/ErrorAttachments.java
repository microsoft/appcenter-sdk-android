package com.microsoft.azure.mobile.crashes;

import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import static com.microsoft.azure.mobile.MobileCenter.LOG_TAG;

/**
 * Error attachment utilities.
 */
public final class ErrorAttachments {

    public static final String CONTENT_TYPE_TEXT_PLAIN  = "text/plain";
    public static final String CONTENT_TYPE_BINARY      = "binary";

    @VisibleForTesting
    ErrorAttachments() {

        /* Utils pattern, hide constructor. */
    }

    /**
     * Build an error attachment log with text suitable for using in {link CrashesListener#getErrorAttachment(ErrorReport)}.
     *
     * @param text      text to attach to attachment log.
     * @param fileName  file name to use in error attachment log.
     * @return ErrorAttachmentLog or null if null text is passed.
     */
    public static ErrorAttachmentLog attachmentWithText(String text, String fileName) {
        return attachment(null, text, fileName, CONTENT_TYPE_TEXT_PLAIN);
    }

    /**
     * Build an error attachment log with binary suitable for using in {link CrashesListener#getErrorAttachment(ErrorReport)}.
     *
     * @param data        binary data.
     * @param fileName    file name to use in error attachment log.
     * @return ErrorAttachmentLog attachment or null if null data is passed.
     */
    public static ErrorAttachmentLog attachmentWithBinary(byte[] data, String fileName) {
        return attachment(data, null, fileName, CONTENT_TYPE_BINARY);
    }

    /**
     * Build an error attachment log with text and binary suitable for using in {link CrashesListener#getErrorAttachment(ErrorReport)}.
     *
     * @param data        binary data.
     * @param text        text to attach to the attachment log.
     * @param fileName    file name to use in error attachment log.
     * @param contentType binary data MIME type.
     * @return ErrorAttachmentLog attachment or null if text and data are null.
     */
    private static ErrorAttachmentLog attachment(byte[] data, String text, String fileName, String contentType) {
        if ((data == null && contentType.equals(CONTENT_TYPE_BINARY)) ||
            (text == null && contentType.equals(CONTENT_TYPE_TEXT_PLAIN))) {
            MobileCenterLog.warn(LOG_TAG, "Null content passed to attachment method, returning null");
            return null;
        }
        if (fileName == null) {
            MobileCenterLog.warn(LOG_TAG, "Null file name passed to attachment method, returning null");
            return null;
        }

        String content = text;
        if(contentType.equals(CONTENT_TYPE_BINARY)){
            content = Base64.encodeToString(data, Base64.DEFAULT);
        }
        ErrorAttachmentLog attachmentLog = new ErrorAttachmentLog();
        attachmentLog.setContentType(contentType);
        attachmentLog.setFileName(fileName);
        attachmentLog.setData(content);
        return attachmentLog;
    }
}
