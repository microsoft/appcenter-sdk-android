package com.microsoft.azure.mobile.crashes.ingestion.models;

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
