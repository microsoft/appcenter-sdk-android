package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class DeviceExtension implements Model {

    /**
     * Make property.
     */
    private static final String MAKE = "make";

    /**
     * Model property.
     */
    private static final String MODEL = "model";

    /**
     * Device manufacturer.
     */
    private String make;

    /**
     * Device model.
     */
    private String model;

    /**
     * Get device make.
     *
     * @return device make.
     */
    public String getMake() {
        return make;
    }

    /**
     * Set device make.
     *
     * @param make device make.
     */
    public void setMake(String make) {
        this.make = make;
    }

    /**
     * Get device model.
     *
     * @return device model.
     */
    public String getModel() {
        return model;
    }

    /**
     * Set device model.
     *
     * @param model device model.
     */
    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setMake(object.getString(MAKE));
        setModel(object.getString(MODEL));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(MAKE).value(getMake());
        writer.key(MODEL).value(getModel());
    }
}
