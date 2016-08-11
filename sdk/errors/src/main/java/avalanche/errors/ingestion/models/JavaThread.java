package avalanche.errors.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import avalanche.core.ingestion.models.Model;
import avalanche.core.ingestion.models.json.JSONUtils;
import avalanche.errors.ingestion.models.json.JavaStackFrameFactory;

import static avalanche.core.ingestion.models.CommonProperties.FRAMES;
import static avalanche.core.ingestion.models.CommonProperties.ID;
import static avalanche.core.ingestion.models.CommonProperties.NAME;

/**
 * The JavaThread model.
 */
public class JavaThread implements Model {

    /**
     * Thread identifier.
     */
    private long id;

    /**
     * Thread name.
     */
    private String name;

    /**
     * Stack frames.
     */
    private List<JavaStackFrame> frames;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public long getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     */
    public void setName(String name) {
        this.name = name;
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
        setId(object.getLong(ID));
        setName(object.optString(NAME, null));
        setFrames(JSONUtils.readArray(object, FRAMES, JavaStackFrameFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, NAME, getName());
        JSONUtils.writeArray(writer, FRAMES, getFrames());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaThread that = (JavaThread) o;

        if (id != that.id) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return frames != null ? frames.equals(that.frames) : that.frames == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (frames != null ? frames.hashCode() : 0);
        return result;
    }
}
