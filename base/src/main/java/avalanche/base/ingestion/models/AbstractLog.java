package avalanche.base.ingestion.models;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import avalanche.base.ingestion.models.utils.LogUtils;

/**
 * The AbstractLog model.
 */
public abstract class AbstractLog implements Log {

    /**
     * toffset property
     */
    private static final String TOFFSET = "toffset";

    /**
     * Corresponds to the number of milliseconds elapsed between the time the
     * request is sent and the time the log is emitted.
     */
    private long toffset;

    @Override
    public long getToffset() {
        return this.toffset;
    }

    @Override
    public void setToffset(long toffset) {
        this.toffset = toffset;
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(CommonProperties.TYPE).value(getType());
        writer.key(TOFFSET).value(getToffset());
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        if (!object.getString(CommonProperties.TYPE).equals(getType()))
            throw new JSONException("Invalid type");
        setToffset(object.getLong(TOFFSET));
    }

    @Override
    public void validate() throws IllegalArgumentException {
        LogUtils.checkNotNull(CommonProperties.TYPE, getType());
        LogUtils.checkNotNull(TOFFSET, getToffset());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractLog that = (AbstractLog) o;

        return toffset == that.toffset;
    }

    @Override
    public int hashCode() {
        return (int) (toffset ^ (toffset >>> 32));
    }
}
