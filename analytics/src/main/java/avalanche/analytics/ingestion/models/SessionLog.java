package avalanche.analytics.ingestion.models;

import avalanche.base.ingestion.models.Log;

/**
 * Session log.
 */
public class SessionLog extends Log {

    /**
     * Unique session identifier. The same identifier must be used for end and
     * start session.
     */
    private String sid;

    /**
     * `true` to mark the end of the session, `false` if it the start of the
     * session.
     */
    private Boolean end;

    @Override
    public String getType() {
        return "session";
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
    public Boolean getEnd() {
        return this.end;
    }

    /**
     * Set the end value.
     *
     * @param end the end value to set
     */
    public void setEnd(Boolean end) {
        this.end = end;
    }
}
