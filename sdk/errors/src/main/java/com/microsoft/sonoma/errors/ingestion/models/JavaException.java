package com.microsoft.sonoma.errors.ingestion.models;

import com.microsoft.sonoma.core.ingestion.models.Model;
import com.microsoft.sonoma.core.ingestion.models.json.JSONUtils;
import com.microsoft.sonoma.errors.ingestion.models.json.JavaStackFrameFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import static com.microsoft.sonoma.core.ingestion.models.CommonProperties.FRAMES;
import static com.microsoft.sonoma.core.ingestion.models.CommonProperties.TYPE;

/**
 * The JavaException model.
 */
public class JavaException implements Model {

    private static final String MESSAGE = "message";

    /**
     * Exception type (fully qualified class name).
     */
    private String type;

    /**
     * Exception message.
     */
    private String message;

    /**
     * Exception stack trace elements.
     */
    private List<JavaStackFrame> frames;

    /**
     * Get the type value.
     *
     * @return the type value
     */
    public String getType() {
        return this.type;
    }

    /**
     * Set the type value.
     *
     * @param type the type value to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the message value.
     *
     * @return the message value
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Set the message value.
     *
     * @param message the message value to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the frames value.
     *
     * @return the frames value
     */
    public List<JavaStackFrame> getFrames() {
        return this.frames;
    }

    /**
     * Set the frames value.
     *
     * @param frames the frames value to set
     */
    public void setFrames(List<JavaStackFrame> frames) {
        this.frames = frames;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setType(object.optString(TYPE, null));
        setMessage(object.optString(MESSAGE, null));
        setFrames(JSONUtils.readArray(object, FRAMES, JavaStackFrameFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, TYPE, getType());
        JSONUtils.write(writer, MESSAGE, getMessage());
        JSONUtils.writeArray(writer, FRAMES, getFrames());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaException that = (JavaException) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return frames != null ? frames.equals(that.frames) : that.frames == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (frames != null ? frames.hashCode() : 0);
        return result;
    }
}
