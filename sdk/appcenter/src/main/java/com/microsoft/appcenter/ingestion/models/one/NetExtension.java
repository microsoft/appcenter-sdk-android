package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Extension for network connectivity information.
 */
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
    public void read(JSONObject object) {
        setProvider(object.optString(PROVIDER, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, PROVIDER, getProvider());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetExtension that = (NetExtension) o;

        return provider != null ? provider.equals(that.provider) : that.provider == null;
    }

    @Override
    public int hashCode() {
        return provider != null ? provider.hashCode() : 0;
    }
}
