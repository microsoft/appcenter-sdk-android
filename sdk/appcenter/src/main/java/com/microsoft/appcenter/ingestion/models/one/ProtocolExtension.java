package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Extension for device specific information.
 */
public class ProtocolExtension implements Model {

    /**
     * Device manufacturer property.
     */
    private static final String DEV_MAKE = "devMake";

    /**
     * Device model property.
     */
    private static final String DEV_MODEL = "devModel";

    /**
     * Device manufacturer.
     */
    private String devMake;

    /**
     * Device model.
     */
    private String devModel;

    /**
     * Get device manufacturer.
     *
     * @return device manufacturer.
     */
    public String getDevMake() {
        return devMake;
    }

    /**
     * Set device manufacturer.
     *
     * @param devMake device manufacturer.
     */
    public void setDevMake(String devMake) {
        this.devMake = devMake;
    }

    /**
     * Get device model.
     *
     * @return device model.
     */
    public String getDevModel() {
        return devModel;
    }

    /**
     * Set device model.
     *
     * @param devModel device model.
     */
    public void setDevModel(String devModel) {
        this.devModel = devModel;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setDevMake(object.optString(DEV_MAKE, null));
        setDevModel(object.optString(DEV_MODEL, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, DEV_MAKE, getDevMake());
        JSONUtils.write(writer, DEV_MODEL, getDevModel());
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProtocolExtension that = (ProtocolExtension) o;

        if (devMake != null ? !devMake.equals(that.devMake) : that.devMake != null) return false;
        return devModel != null ? devModel.equals(that.devModel) : that.devModel == null;
    }

    @Override
    public int hashCode() {
        int result = devMake != null ? devMake.hashCode() : 0;
        result = 31 * result + (devModel != null ? devModel.hashCode() : 0);
        return result;
    }
}
