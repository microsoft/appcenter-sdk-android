package avalanche.crash.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import avalanche.base.ingestion.models.Model;
import avalanche.base.ingestion.models.json.JSONUtils;
import avalanche.crash.ingestion.models.json.ThreadFrameFactory;

import static avalanche.base.ingestion.models.CommonProperties.FRAMES;
import static avalanche.base.ingestion.models.CommonProperties.ID;
import static avalanche.base.ingestion.models.CommonProperties.TYPE;

/**
 * The Exception model.
 */
public class Exception implements Model {

    private static final String REASON = "reason";

    private static final String PLATFORM = "platform";

    private static final String OUTER_ID = "outerId";

    /**
     * number of the exception.
     */
    private Integer id;

    /**
     * number of the outer exception, 0 for root exception.
     */
    private Integer outerId;

    /**
     * reason string of the exception.
     */
    private String reason;

    /**
     * Possible values include: 'Android', 'iOS', 'Xamarin', 'UWP'.
     */
    private String platform;

    /**
     * managed or native exception. Possible values include: 'managed',
     * 'native'.
     */
    private String type;

    /**
     * Exception stack trace frames.
     */
    private List<ThreadFrame> frames;

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
     * Get the outerId value.
     *
     * @return the outerId value
     */
    public Integer getOuterId() {
        return this.outerId;
    }

    /**
     * Set the outerId value.
     *
     * @param outerId the outerId value to set
     */
    public void setOuterId(Integer outerId) {
        this.outerId = outerId;
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
     * Get the platform value.
     *
     * @return the platform value
     */
    public String getPlatform() {
        return this.platform;
    }

    /**
     * Set the platform value.
     *
     * @param platform the platform value to set
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }

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

    @Override
    public void read(JSONObject object) throws JSONException {
        setId(JSONUtils.readInteger(object, ID));
        setOuterId(JSONUtils.readInteger(object, OUTER_ID));
        setReason(object.optString(REASON, null));
        setPlatform(object.optString(PLATFORM, null));
        setType(object.optString(TYPE, null));
        setFrames(JSONUtils.readArray(object, FRAMES, ThreadFrameFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ID, getId(), false);
        JSONUtils.write(writer, OUTER_ID, getOuterId(), false);
        JSONUtils.write(writer, REASON, getReason(), false);
        JSONUtils.write(writer, PLATFORM, getPlatform(), false);
        JSONUtils.write(writer, TYPE, getType(), false);
        JSONUtils.writeArray(writer, FRAMES, getFrames());
    }

    @Override
    public void validate() throws IllegalArgumentException {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Exception exception = (Exception) o;

        if (id != null ? !id.equals(exception.id) : exception.id != null) return false;
        if (outerId != null ? !outerId.equals(exception.outerId) : exception.outerId != null)
            return false;
        if (reason != null ? !reason.equals(exception.reason) : exception.reason != null)
            return false;
        if (platform != null ? !platform.equals(exception.platform) : exception.platform != null)
            return false;
        if (type != null ? !type.equals(exception.type) : exception.type != null) return false;
        return frames != null ? frames.equals(exception.frames) : exception.frames == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (outerId != null ? outerId.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (frames != null ? frames.hashCode() : 0);
        return result;
    }
}
