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

    /**
     * Plain text mime type
     */
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    @VisibleForTesting
    ErrorAttachments() {

        /* Utils pattern, hide constructor. */
    }

    /**
     * Build an error attachment log with text suitable for using in {link CrashesListener#getErrorAttachments(ErrorReport)}.
     *
     * @param text     text to attach to attachment log.
     * @param fileName file name to use in error attachment log.
     * @return ErrorAttachmentLog or null if null text is passed.
     */
    public static ErrorAttachmentLog attachmentWithText(String text, String fileName) {
        return attachment(null, text, fileName, CONTENT_TYPE_TEXT_PLAIN);
    }

    /**
     * Build an error attachment log with binary suitable for using in {link CrashesListener#getErrorAttachments(ErrorReport)}.
     *
     * @param data     binary data.
     * @param fileName file name to use in error attachment log.
     * @return ErrorAttachmentLog attachment or null if null data is passed.
     */
    public static ErrorAttachmentLog attachmentWithBinary(byte[] data, String fileName, String contentType) {
        return attachment(data, null, fileName, contentType);
    }

    /**
     * Build an error attachment log with text and binary suitable for using in {link CrashesListener#getErrorAttachments(ErrorReport)}.
     *
     * @param data        binary data.
     * @param text        text to attach to the attachment log.
     * @param fileName    file name to use in error attachment log.
     * @param contentType binary data MIME type.
     * @return ErrorAttachmentLog attachment or null if text and data are null.
     */
    private static ErrorAttachmentLog attachment(byte[] data, String text, String fileName, String contentType) {
        String content = text;
        if (isBinaryContentType(contentType)) {
            content = Base64.encodeToString(data, Base64.DEFAULT);
        }
        ErrorAttachmentLog attachmentLog = new ErrorAttachmentLog();
        attachmentLog.setContentType(contentType);
        attachmentLog.setFileName(fileName);
        attachmentLog.setData(content);
        return attachmentLog;
    }

    /**
     * Checks if content type provided by user is binary.
     *
     * @param contentType content type.
     * @return True if binary, otherwise False.
     */
    private static boolean isBinaryContentType(String contentType) {
        return !contentType.startsWith("text/");
    }

    /**
     * Validates ErrorAttachmentLog
     *
     * @param log ErrorAttachmentLog to validate.
     * @return true if validation succeeded, otherwise false.
     */
    static boolean validateErrorAttachmentLog(ErrorAttachmentLog log) {
        if (log == null) {
            MobileCenterLog.error(LOG_TAG, "Null ErrorAttachmentLog passed to validate method");
            return false;
        }
        if (log.getId() == null || log.getErrorId() == null || log.getContentType() == null ||
                log.getFileName() == null || log.getData() == null) {
            MobileCenterLog.error(LOG_TAG, "Not all required fields are present in ErrorAttachmentLog.");
            return false;
        }
        return true;
    }
}
