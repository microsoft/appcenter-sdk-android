/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.VALUE;

public class StringTypedProperty extends TypedProperty {

    public static final String TYPE = "string";

    /**
     * Property value.
     */
    private String value;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the property value.
     *
     * @return the property value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the property value.
     *
     * @param value the property value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setValue(object.getString(VALUE));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(VALUE).value(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StringTypedProperty that = (StringTypedProperty) o;

        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
