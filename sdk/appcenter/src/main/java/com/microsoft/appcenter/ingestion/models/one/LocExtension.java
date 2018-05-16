package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

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
        setTz(object.getString(TZ));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(TZ).value(getTz());
    }
}
