package com.microsoft.sonoma.crashes.model;

import com.microsoft.sonoma.core.ingestion.models.Model;
import com.microsoft.sonoma.core.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Attachment for error log.
 */
public class ErrorAttachment implements Model {

    /**
     * textAttachment property.
     */
    private static final String TEXT_ATTACHMENT = "text_attachment";

    /**
     * binaryAttachment property.
     */
    private static final String BINARY_ATTACHMENT = "binary_attachment";

    /**
     * Plain text attachment.
     */
    private String textAttachment;

    /**
     * Binary attachment.
     */
    private ErrorBinaryAttachment binaryAttachment;

    /**
     * Get the textAttachment value.
     *
     * @return the textAttachment value
     */
    public String getTextAttachment() {
        return this.textAttachment;
    }

    /**
     * Set the textAttachment value.
     *
     * @param textAttachment the textAttachment value to set
     */
    public void setTextAttachment(String textAttachment) {
        this.textAttachment = textAttachment;
    }

    /**
     * Get the binaryAttachment value.
     *
     * @return the binaryAttachment value
     */
    public ErrorBinaryAttachment getBinaryAttachment() {
        return this.binaryAttachment;
    }

    /**
     * Set the binaryAttachment value.
     *
     * @param binaryAttachment the binaryAttachment value to set
     */
    public void setBinaryAttachment(ErrorBinaryAttachment binaryAttachment) {
        this.binaryAttachment = binaryAttachment;
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, TEXT_ATTACHMENT, getTextAttachment());
        if (getBinaryAttachment() != null) {
            writer.key(BINARY_ATTACHMENT).object();
            getBinaryAttachment().write(writer);
            writer.endObject();
        }
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setTextAttachment(object.optString(TEXT_ATTACHMENT));
        if (object.has(BINARY_ATTACHMENT)) {
            ErrorBinaryAttachment binaryAttachment = new ErrorBinaryAttachment();
            binaryAttachment.read(object.getJSONObject(BINARY_ATTACHMENT));
            setBinaryAttachment(binaryAttachment);
        }
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorAttachment that = (ErrorAttachment) o;

        if (textAttachment != null ? !textAttachment.equals(that.textAttachment) : that.textAttachment != null)
            return false;
        return binaryAttachment != null ? binaryAttachment.equals(that.binaryAttachment) : that.binaryAttachment == null;

    }

    @Override
    public int hashCode() {
        int result = textAttachment != null ? textAttachment.hashCode() : 0;
        result = 31 * result + (binaryAttachment != null ? binaryAttachment.hashCode() : 0);
        return result;
    }
}
