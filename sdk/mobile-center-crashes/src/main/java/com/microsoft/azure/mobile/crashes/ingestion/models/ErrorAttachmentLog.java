package com.microsoft.azure.mobile.crashes.ingestion.models;

import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import com.microsoft.azure.mobile.ingestion.models.AbstractLog;
import com.microsoft.azure.mobile.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import static com.microsoft.azure.mobile.ingestion.models.CommonProperties.ID;

/**
 * Error attachment log.
 */
public class ErrorAttachmentLog extends AbstractLog {

    public static final String TYPE = "error_attachment";

    @VisibleForTesting
    static final Charset CHARSET = Charset.forName("UTF-8");

    @VisibleForTesting
    static final String DATA = "data";

    private static final String ERROR_ID = "error_id";

    private static final String FILE_NAME = "file_name";

    /**
     * Error attachment identifier.
     */
    private UUID id;

    /**
     * Error log identifier to attach this log to.
     */
    private UUID errorId;

    /**
     * File name.
     */
    private String fileName;

    /**
     * Data encoded as base64.
     */
    private byte[] data;

    /**
     * Build an error attachment log with text suitable for using in {link CrashesListener#getErrorAttachments(ErrorReport)}.
     *
     * @param text     text to attach to attachment log.
     * @param fileName file name to use in error attachment log.
     * @return ErrorAttachmentLog or null if null text is passed.
     */
    public static ErrorAttachmentLog attachmentWithText(String text, String fileName) {
        return attachmentWithBinary(text.getBytes(CHARSET), fileName);
    }

    /**
     * Build an error attachment log with binary suitable for using in {link CrashesListener#getErrorAttachments(ErrorReport)}.
     *
     * @param data     binary data.
     * @param fileName file name to use in error attachment log.
     * @return ErrorAttachmentLog attachment or null if null data is passed.
     */
    public static ErrorAttachmentLog attachmentWithBinary(byte[] data, String fileName) {
        ErrorAttachmentLog attachmentLog = new ErrorAttachmentLog();
        attachmentLog.setData(data);
        attachmentLog.setFileName(fileName);
        return attachmentLog;
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
    @SuppressWarnings("WeakerAccess")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get the data value.
     *
     * @return the data value
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Set the data value.
     *
     * @param data the data value to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Checks if the log's values are valid.
     *
     * @return true if validation succeeded, otherwise false.
     */
    public boolean isValid() {
        return getId() != null && getErrorId() != null && getData() != null;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
        setErrorId(UUID.fromString(object.getString(ERROR_ID)));
        setFileName(object.optString(FILE_NAME, null));
        try {
            setData(Base64.decode(object.getString(DATA), Base64.DEFAULT));
        } catch (IllegalArgumentException e) {
            throw new JSONException(e.getMessage());
        }
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, ERROR_ID, getErrorId());
        JSONUtils.write(writer, FILE_NAME, getFileName());
        JSONUtils.write(writer, DATA, Base64.encodeToString(getData(), Base64.DEFAULT));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ErrorAttachmentLog that = (ErrorAttachmentLog) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (errorId != null ? !errorId.equals(that.errorId) : that.errorId != null) return false;
        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null)
            return false;
        return Arrays.equals(data, that.data);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (errorId != null ? errorId.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
