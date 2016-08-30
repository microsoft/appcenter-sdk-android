package com.microsoft.sonoma.errors;

import android.support.annotation.VisibleForTesting;

import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.errors.model.ErrorAttachment;
import com.microsoft.sonoma.errors.model.ErrorBinaryAttachment;
import com.microsoft.sonoma.errors.model.ErrorReport;

import static com.microsoft.sonoma.core.Sonoma.LOG_TAG;

/**
 * Error attachment utilities.
 */
public final class ErrorAttachments {

    @VisibleForTesting
    ErrorAttachments() {

        /* Utils pattern, hide constructor. */
    }

    /**
     * Build an attachment with text suitable for using in {@link com.microsoft.sonoma.errors.ErrorReportingListener#getErrorAttachment(ErrorReport)}.
     *
     * @param text text to attach to error report.
     * @return error attachment or null if null text is passed.
     */
    public static ErrorAttachment attachmentWithText(String text) {
        return attachment(text, null, null, null);
    }

    /**
     * Build an attachment with binary suitable for using in {@link com.microsoft.sonoma.errors.ErrorReportingListener#getErrorAttachment(ErrorReport)}.
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
     * Build an attachment with text and binary suitable for using in {@link com.microsoft.sonoma.errors.ErrorReportingListener#getErrorAttachment(ErrorReport)}.
     *
     * @param text        text data.
     * @param data        binary data.
     * @param fileName    file name to use on reports for the binary data.
     * @param contentType binary data MIME type.
     * @return error attachment or null if text and data are null.
     */
    public static ErrorAttachment attachment(String text, byte[] data, String fileName, String contentType) {
        if (text == null && data == null) {
            SonomaLog.warn(LOG_TAG, "Null content passed to attachment method, returning null");
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
            SonomaLog.warn(LOG_TAG, "Binary attachment file name and content ignored as data is null");
        }
        return attachment;
    }
}
