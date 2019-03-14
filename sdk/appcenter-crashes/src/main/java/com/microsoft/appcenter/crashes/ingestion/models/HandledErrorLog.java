/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models;

import com.microsoft.appcenter.ingestion.models.LogWithProperties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.UUID;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.ID;

/**
 * Handled Error log for managed platforms (such as Xamarin, Unity, Android Dalvik/ART).
 */
public class HandledErrorLog extends LogWithProperties {

    /**
     * Log type.
     */
    public static final String TYPE = "handledError";

    /**
     * Exception associated to the error.
     */
    private static final String EXCEPTION = "exception";

    /**
     * Unique identifier for this error.
     */
    private UUID id;

    /**
     * Exception associated to the error.
     */
    private Exception exception;

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
     * Get the exception value.
     *
     * @return the exception value
     */
    public Exception getException() {
        return this.exception;
    }

    /**
     * Set the exception value.
     *
     * @param exception the exception value to set
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
        if (object.has(EXCEPTION)) {
            JSONObject jException = object.getJSONObject(EXCEPTION);
            Exception exception = new Exception();
            exception.read(jException);
            setException(exception);
        }
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(ID).value(getId());
        if (getException() != null) {
            writer.key(EXCEPTION).object();
            exception.write(writer);
            writer.endObject();
        }
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        HandledErrorLog errorLog = (HandledErrorLog) o;

        if (id != null ? !id.equals(errorLog.id) : errorLog.id != null) return false;
        return exception != null ? exception.equals(errorLog.exception) : errorLog.exception == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }
}
