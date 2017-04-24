package com.microsoft.azure.mobile.crashes.ingestion.models;

import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import com.microsoft.azure.mobile.ingestion.models.AbstractLog;
import com.microsoft.azure.mobile.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.UUID;

import static com.microsoft.azure.mobile.ingestion.models.CommonProperties.ID;

/**
 * Error attachment log.
 */
public class ErrorAttachmentLog extends AbstractLog {

    /**
     * Plain text mime type.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    public static final String TYPE = "error_attachment";

    private static final String ERROR_ID = "error_id";

    private static final String CONTENT_TYPE = "content_type";

    private static final String FILE_NAME = "file_name";

    private static final String DATA = "data";

    /**
     * Error attachment identifier.
     */
    private UUID id;

    /**
     * Error log identifier to attach this log to.
     */
    private UUID errorId;

    /**
     * Content type (text/plain for text).
     */
    private String contentType;

    /**
     * File name.
     */
    private String fileName;

    /**
     * Data (plain text or base64 string for binary data).
     */
    private String data;

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
     * @return true if binary, otherwise false.
     */
    @VisibleForTesting
    static boolean isBinaryContentType(String contentType) {
        return contentType != null && !contentType.startsWith("text/");
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Get the errorId value.
     *
     * @return the errorId value
     */
    public UUID getErrorId() {
        return this.errorId;
    }

    /**
     * Set the errorId value.
     *
     * @param errorId the errorId value to set
     */
    public void setErrorId(UUID errorId) {
        this.errorId = errorId;
    }

    /**
     * Get the contentType value.
     *
     * @return the contentType value
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Set the contentType value.
     *
     * @param contentType the contentType value to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Get the fileName value.
     *
     * @return the fileName value
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Set the fileName value.
     *
     * @param fileName the fileName value to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get the data value.
     *
     * @return the data value
     */
    public String getData() {
        return this.data;
    }

    /**
     * Set the data value.
     *
     * @param data the data value to set
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Checks if the log's values are valid.
     *
     * @return true if validation succeeded, otherwise false.
     */
    public boolean isValid() {
        return getId() != null && getErrorId() != null && getContentType() != null && getData() != null;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
        setErrorId(UUID.fromString(object.getString(ERROR_ID)));
        setContentType(object.getString(CONTENT_TYPE));
        setFileName(object.optString(FILE_NAME, null));
        setData(object.getString(DATA));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, ERROR_ID, getErrorId());
        JSONUtils.write(writer, CONTENT_TYPE, getContentType());
        JSONUtils.write(writer, FILE_NAME, getFileName());
        JSONUtils.write(writer, DATA, getData());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ErrorAttachmentLog that = (ErrorAttachmentLog) o;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (errorId != null ? !errorId.equals(that.errorId) : that.errorId != null) return false;
        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;
        return data != null ? data.equals(that.data) : that.data == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (errorId != null ? errorId.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
