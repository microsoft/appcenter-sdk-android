package avalanche.crash.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import avalanche.base.ingestion.models.Model;
import avalanche.base.ingestion.models.json.JSONUtils;
import avalanche.base.ingestion.models.utils.LogUtils;
import avalanche.crash.ingestion.models.json.ExceptionFactory;
import avalanche.crash.ingestion.models.json.ThreadFrameFactory;

import static avalanche.base.ingestion.models.CommonProperties.FRAMES;
import static avalanche.base.ingestion.models.CommonProperties.ID;

/**
 * The Exception model.
 */
public class Exception implements Model {

    private static final String REASON = "reason";

    private static final String LANGUAGE = "language";

    private static final String INNER_EXCEPTIONS = "innerExceptions";

    /**
     * number of the exception.
     */
    private Integer id;

    /**
     * reason string of the exception.
     */
    private String reason;

    /**
     * Possible values include: 'Java', 'C#'.
     */
    private String language;

    /**
     * Exception stack trace frames.
     */
    private List<ThreadFrame> frames;

    /**
     * Inner exceptions.
     */
    private List<Exception> innerExceptions;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Get the reason value.
     *
     * @return the reason value
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Set the reason value.
     *
     * @param reason the reason value to set
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Get the language value.
     *
     * @return the language value
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Set the language value.
     *
     * @param language the language value to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Get the frames value.
     *
     * @return the frames value
     */
    public List<ThreadFrame> getFrames() {
        return this.frames;
    }

    /**
     * Set the frames value.
     *
     * @param frames the frames value to set
     */
    public void setFrames(List<ThreadFrame> frames) {
        this.frames = frames;
    }

    /**
     * Get the innerExceptions value.
     *
     * @return the innerExceptions value
     */
    public List<Exception> getInnerExceptions() {
        return this.innerExceptions;
    }

    /**
     * Set the innerExceptions value.
     *
     * @param innerExceptions the innerExceptions value to set
     */
    public void setInnerExceptions(List<Exception> innerExceptions) {
        this.innerExceptions = innerExceptions;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setId(JSONUtils.readInteger(object, ID));
        setReason(object.optString(REASON, null));
        setLanguage(object.optString(LANGUAGE, null));
        setFrames(JSONUtils.readArray(object, FRAMES, ThreadFrameFactory.getInstance()));
        setInnerExceptions(JSONUtils.readArray(object, INNER_EXCEPTIONS, ExceptionFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ID, getId(), false);
        JSONUtils.write(writer, REASON, getReason(), false);
        JSONUtils.write(writer, LANGUAGE, getLanguage(), false);
        JSONUtils.writeArray(writer, FRAMES, getFrames());
        JSONUtils.writeArray(writer, INNER_EXCEPTIONS, getInnerExceptions());
    }

    @Override
    public void validate() throws IllegalArgumentException {
        LogUtils.validateArray(getFrames());
        LogUtils.validateArray(getInnerExceptions());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Exception exception = (Exception) o;

        if (id != null ? !id.equals(exception.id) : exception.id != null) return false;
        if (reason != null ? !reason.equals(exception.reason) : exception.reason != null)
            return false;
        if (language != null ? !language.equals(exception.language) : exception.language != null)
            return false;
        if (frames != null ? !frames.equals(exception.frames) : exception.frames != null)
            return false;
        return innerExceptions != null ? innerExceptions.equals(exception.innerExceptions) : exception.innerExceptions == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (frames != null ? frames.hashCode() : 0);
        result = 31 * result + (innerExceptions != null ? innerExceptions.hashCode() : 0);
        return result;
    }
}
