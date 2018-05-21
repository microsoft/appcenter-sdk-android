package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Describes the location from which the event was logged.
 */
public class LocExtension implements Model {

    /**
     * Time zone property.
     */
    private static final String TZ = "tz";

    /**
     * Time zone on the device.
     */
    private String tz;

    /**
     * Get device time zone.
     *
     * @return device time zone.
     */
    public String getTz() {
        return tz;
    }

    /**
     * Set device time zone.
     *
     * @param tz device time zone.
     */
    public void setTz(String tz) {
        this.tz = tz;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setTz(object.optString(TZ, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, TZ, getTz());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocExtension that = (LocExtension) o;

        return tz != null ? tz.equals(that.tz) : that.tz == null;
    }

    @Override
    public int hashCode() {
        return tz != null ? tz.hashCode() : 0;
    }
}
