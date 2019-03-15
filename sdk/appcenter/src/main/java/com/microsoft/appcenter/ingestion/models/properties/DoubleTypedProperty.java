/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.VALUE;

public class DoubleTypedProperty extends TypedProperty {

    public static final String TYPE = "double";

    /**
     * Property value.
     */
    private double value;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the property value.
     *
     * @return the property value
     */
    public double getValue() {
        return value;
    }

    /**
     * Set the property value.
     *
     * @param value the property value to set
     */
    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setValue(object.getDouble(VALUE));
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

        DoubleTypedProperty that = (DoubleTypedProperty) o;

        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
