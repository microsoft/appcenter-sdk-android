package avalanche.crash.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import avalanche.base.ingestion.models.CommonProperties;
import avalanche.base.ingestion.models.Definition;
import avalanche.base.ingestion.models.json.JSONUtils;
import avalanche.base.ingestion.models.utils.LogUtils;
import avalanche.crash.ingestion.models.json.ThreadFrameFactory;

import static avalanche.base.ingestion.models.CommonProperties.ID;

/**
 * The Thread model.
 */
public class Thread implements Definition {

    /**
     * Thread number.
     */
    private int id;

    /**
     * Thread frames.
     */
    private List<ThreadFrame> frames;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public int getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(int id) {
        this.id = id;
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
        setId(object.getInt(ID));
        setFrames(JSONUtils.readArray(object, CommonProperties.FRAMES, ThreadFrameFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(ID).value(getId());
        JSONUtils.writeArray(writer, CommonProperties.FRAMES, getFrames());
    }

    @Override
    public void validate() throws IllegalArgumentException {
        LogUtils.checkNotNull(ID, getId());
        LogUtils.checkNotNull(CommonProperties.FRAMES, getFrames());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Thread thread = (Thread) o;

        if (id != thread.id) return false;
        return frames != null ? frames.equals(thread.frames) : thread.frames == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (frames != null ? frames.hashCode() : 0);
        return result;
    }
}
