package com.microsoft.azure.mobile.crashes.ingestion.models;

import com.microsoft.azure.mobile.ingestion.models.AbstractLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Handled Error log for managed platforms (such as Xamarin, Unity, Android Dalvik/ART).
 */
public class HandledErrorLog extends AbstractLog {

    /**
     * Log type.
     */
    public static final String TYPE = "handled_error";

    /**
     * Exception associated to the error.
     */
    private static final String EXCEPTION = "exception";

    /**
     * Exception associated to the error.
     */
    private Exception exception;

    @Override
    public String getType() {
        return TYPE;
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

        HandledErrorLog that = (HandledErrorLog) o;

        return exception != null ? exception.equals(that.exception) : that.exception == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }
}
