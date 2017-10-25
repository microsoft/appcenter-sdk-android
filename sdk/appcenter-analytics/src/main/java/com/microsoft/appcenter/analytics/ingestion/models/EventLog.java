package com.microsoft.appcenter.analytics.ingestion.models;

import com.microsoft.appcenter.ingestion.models.LogWithProperties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.UUID;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.ID;
import static com.microsoft.appcenter.ingestion.models.CommonProperties.NAME;

/**
 * Event log.
 */
public class EventLog extends LogWithProperties {

    public static final String TYPE = "event";

    /**
     * Unique identifier for this event.
     */
    private UUID id;

    /**
     * Name of the event.
     */
    private String name;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the id value.
     *
     * @return the id value
     */
    @SuppressWarnings("WeakerAccess")
    public UUID getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    @SuppressWarnings("WeakerAccess")
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

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
        setName(object.getString(NAME));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(ID).value(getId());
        writer.key(NAME).value(getName());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EventLog eventLog = (EventLog) o;
        if (id != null ? !id.equals(eventLog.id) : eventLog.id != null) {
            return false;
        }
        return name != null ? name.equals(eventLog.name) : eventLog.name == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
