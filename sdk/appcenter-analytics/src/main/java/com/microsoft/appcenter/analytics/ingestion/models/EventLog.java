/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.ingestion.models;

import com.microsoft.appcenter.ingestion.models.json.JSONUtils;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedPropertyUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;
import java.util.UUID;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.ID;
import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPED_PROPERTIES;

/**
 * Event log.
 */
public class EventLog extends LogWithNameAndProperties {

    public static final String TYPE = "event";

    /**
     * Unique identifier for this event.
     */
    private UUID id;

    /**
     * Typed properties.
     */
    private List<TypedProperty> typedProperties;

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
     * Get the typedProperties value.
     *
     * @return the typedProperties value
     */
    public List<TypedProperty> getTypedProperties() {
        return typedProperties;
    }

    /**
     * Set the typedProperties value.
     *
     * @param typedProperties the typedProperties value to set
     */
    public void setTypedProperties(List<TypedProperty> typedProperties) {
        this.typedProperties = typedProperties;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
        setTypedProperties(TypedPropertyUtils.read(object));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(ID).value(getId());
        JSONUtils.writeArray(writer, TYPED_PROPERTIES, getTypedProperties());
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EventLog eventLog = (EventLog) o;

        if (id != null ? !id.equals(eventLog.id) : eventLog.id != null) return false;
        return typedProperties != null ? typedProperties.equals(eventLog.typedProperties) : eventLog.typedProperties == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (typedProperties != null ? typedProperties.hashCode() : 0);
        return result;
    }
}
