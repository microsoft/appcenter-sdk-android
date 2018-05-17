package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class NetExtension implements Model {

    /**
     * Network provider property.
     */
    private static final String PROVIDER = "provider";

    /**
     * Network provider on the device.
     */
    private String provider;

    /**
     * Get device network provider.
     *
     * @return device time zone.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Set device network provider.
     *
     * @param provider device network provider.
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setProvider(object.getString(PROVIDER));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(PROVIDER).value(getProvider());
    }
}
