package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Describes the location from which the event was logged.
 */
public class LocationExtension implements Model {

    /**
     * Time zone property.
     */
    private static final String TIMEZONE = "timeZone";

    /**
     * Time zone on the device.
     */
    private String timeZone;

    /**
     * Get device time zone.
     *
     * @return device time zone.
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Set device time zone.
     *
     * @param timeZone device time zone.
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setTimeZone(object.optString(TIMEZONE, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, TIMEZONE, getTimeZone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationExtension that = (LocationExtension) o;

        return timeZone != null ? timeZone.equals(that.timeZone) : that.timeZone == null;
    }

    @Override
    public int hashCode() {
        return timeZone != null ? timeZone.hashCode() : 0;
    }
}
