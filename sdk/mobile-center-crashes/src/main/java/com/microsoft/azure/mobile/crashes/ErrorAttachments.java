package com.microsoft.azure.mobile.crashes;

import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.crashes.model.ErrorAttachment;
import com.microsoft.azure.mobile.crashes.model.ErrorBinaryAttachment;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import static com.microsoft.azure.mobile.MobileCenter.LOG_TAG;

/**
 * Error attachment utilities.
 */
@SuppressWarnings("WeakerAccess")
/* TODO (getErrorAttachment): Re-enable error attachment in javadoc when the feature becomes available. Add @ before link. */
final class ErrorAttachments {

    @VisibleForTesting
    ErrorAttachments() {

        /* Utils pattern, hide constructor. */
    }

    /**
     * Build an attachment with text suitable for using in {link CrashesListener#getErrorAttachment(ErrorReport)}.
     *
     * @param text Text to attach to the error report.
     * @return error Attachment or null if null text is passed.
     */
    public static ErrorAttachment attachmentWithText(String text) {
        return attachment(text, null, null, null);
    }

    /**
     * Build an attachment with binary suitable for using in {link CrashesListener#getErrorAttachment(ErrorReport)}.
     *
     * @param data        binary data.
     * @param fileName    file name to use on reports.
     * @param contentType data MIME type.
     * @return error attachment or null if null data is passed.
     */
    public static ErrorAttachment attachmentWithBinary(byte[] data, String fileName, String contentType) {
        return attachment(null, data, fileName, contentType);
    }

    /**
     * Build an attachment with text and binary suitable for using in {link CrashesListener#getErrorAttachment(ErrorReport)}.
     *
     * @param text        text data.
     * @param data        binary data.
     * @param fileName    file name to use on reports for the binary data.
     * @param contentType binary data MIME type.
     * @return error attachment or null if text and data are null.
     */
    public static ErrorAttachment attachment(String text, byte[] data, String fileName, String contentType) {
        if (text == null && data == null) {
            MobileCenterLog.warn(LOG_TAG, "Null content passed to attachment method, returning null");
            return null;
        }
        ErrorAttachment attachment = new ErrorAttachment();
        attachment.setTextAttachment(text);
        if (data != null) {
            ErrorBinaryAttachment binaryAttachment = new ErrorBinaryAttachment();
            binaryAttachment.setData(data);
            binaryAttachment.setFileName(fileName);
            binaryAttachment.setContentType(contentType);
            attachment.setBinaryAttachment(binaryAttachment);
        } else if (fileName != null || contentType != null) {
            MobileCenterLog.warn(LOG_TAG, "Binary attachment file name and content ignored as data is null");
        }
        return attachment;
    }
}
