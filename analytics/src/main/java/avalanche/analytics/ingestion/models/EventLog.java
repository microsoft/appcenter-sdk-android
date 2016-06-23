package avalanche.analytics.ingestion.models;

import avalanche.base.ingestion.models.InSessionLog;

/**
 * Event log.
 */
public class EventLog extends InSessionLog {

    /**
     * Unique identifier for this event.
     */
    private String id;

    /**
     * Name of the event.
     */
    private String name;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public String getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(String id) {
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
}
