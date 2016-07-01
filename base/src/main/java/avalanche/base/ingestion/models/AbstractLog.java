package avalanche.base.ingestion.models;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.UUID;

import avalanche.base.ingestion.models.utils.LogUtils;

import static avalanche.base.ingestion.models.CommonProperties.TYPE;

/**
 * The AbstractLog model.
 */
public abstract class AbstractLog implements Log {

    /**
     * Session identifier property.
     */
    public static final String SID = "sid";

    /**
     * toffset property.
     */
    private static final String TOFFSET = "toffset";

    /**
     * Corresponds to the number of milliseconds elapsed between the time the
     * request is sent and the time the log is emitted.
     */
    private long toffset;

    /**
     * The session identifier that was provided when the session was started.
     */
    private UUID sid;

    @Override
    public long getToffset() {
        return this.toffset;
    }

    @Override
    public void setToffset(long toffset) {
        this.toffset = toffset;
    }

    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    public UUID getSid() {
        return this.sid;
    }

    /**
     * Set the sid value.
     *
     * @param sid the sid value to set
     */
    public void setSid(UUID sid) {
        this.sid = sid;
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(TYPE).value(getType());
        writer.key(TOFFSET).value(getToffset());
        writer.key(SID).value(getSid());
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        if (!object.getString(TYPE).equals(getType()))
            throw new JSONException("Invalid type");
        setToffset(object.getLong(TOFFSET));
        setSid(UUID.fromString(object.getString(SID)));
    }

    @Override
    public void validate() throws IllegalArgumentException {
        LogUtils.checkNotNull(TYPE, getType());
        LogUtils.checkNotNull(TOFFSET, getToffset());
        LogUtils.checkNotNull(SID, getSid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractLog that = (AbstractLog) o;

        if (toffset != that.toffset) return false;
        return sid != null ? sid.equals(that.sid) : that.sid == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (toffset ^ (toffset >>> 32));
        result = 31 * result + (sid != null ? sid.hashCode() : 0);
        return result;
    }
}
