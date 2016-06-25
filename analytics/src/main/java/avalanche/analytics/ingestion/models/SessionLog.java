package avalanche.analytics.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import avalanche.base.ingestion.models.AbstractLog;
import avalanche.base.ingestion.models.json.JSONUtils;
import avalanche.base.ingestion.models.utils.LogUtils;

import static avalanche.base.ingestion.models.CommonProperties.SID;

/**
 * Session log.
 */
public class SessionLog extends AbstractLog {

    public static final String TYPE = "session";

    private static final String END = "end";

    /**
     * Unique session identifier. The same identifier must be used for end and
     * start session.
     */
    private String sid;

    /**
     * `true` to mark the end of the session, `false` if it the start of the
     * session.
     */
    private boolean end;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    public String getSid() {
        return this.sid;
    }

    /**
     * Set the sid value.
     *
     * @param sid the sid value to set
     */
    public void setSid(String sid) {
        this.sid = sid;
    }

    /**
     * Get the end value.
     *
     * @return the end value
     */
    public boolean isEnd() {
        return this.end;
    }

    /**
     * Set the end value.
     *
     * @param end the end value to set
     */
    public void setEnd(boolean end) {
        this.end = end;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setSid(object.getString(SID));
        setEnd(object.optBoolean(END));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(SID).value(getSid());
        JSONUtils.write(writer, END, isEnd(), false);
    }

    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();
        LogUtils.checkNotNull(SID, getSid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SessionLog that = (SessionLog) o;

        if (end != that.end) return false;
        return sid != null ? sid.equals(that.sid) : that.sid == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sid != null ? sid.hashCode() : 0);
        result = 31 * result + (end ? 1 : 0);
        return result;
    }
}
