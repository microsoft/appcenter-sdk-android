package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

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
        setDevMake(object.getString(DEV_MAKE));
        setDevModel(object.getString(DEV_MODEL));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(DEV_MAKE).value(getDevMake());
        writer.key(DEV_MODEL).value(getDevModel());
    }

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
