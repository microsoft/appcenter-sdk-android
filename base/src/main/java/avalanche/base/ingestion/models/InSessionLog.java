package avalanche.base.ingestion.models;

import java.util.Map;

/**
 * The InSessionLog model.
 */
public abstract class InSessionLog extends Log {

    /**
     * Additional key/value pair parameters.
     */
    private Map<String, String> properties;

    /**
     * The session identifier that was provided when the session was started.
     */
    private String sid;

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * Set the properties value.
     *
     * @param properties the properties value to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
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
}
