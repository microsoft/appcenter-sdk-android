package com.microsoft.appcenter.analytics.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.UUID;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.ID;

/**
 * Event log.
 */
public class EventLog extends LogWithNameAndProperties {

    public static final String TYPE = "event";

    /**
     * Unique identifier for this event.
     */
    private UUID id;

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

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(ID).value(getId());
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
        return id != null ? id.equals(eventLog.id) : eventLog.id == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
