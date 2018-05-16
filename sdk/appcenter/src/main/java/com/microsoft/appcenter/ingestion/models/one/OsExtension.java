package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class OsExtension implements Model {

    /**
     * Name property.
     */
    private static final String NAME = "name";

    /**
     * Ver property.
     */
    private static final String VER = "ver";

    /**
     * Os name.
     */
    private String name;

    /**
     * Os version.
     */
    private String ver;

    /**
     * Get os name.
     *
     * @return os name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set os name.
     *
     * @param name os name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get os version.
     *
     * @return os version.
     */
    public String getVer() {
        return ver;
    }

    /**
     * Set os version.
     *
     * @param ver os version.
     */
    public void setVer(String ver) {
        this.ver = ver;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setName(object.getString(NAME));
        setVer(object.getString(VER));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(NAME).value(getName());
        writer.key(VER).value(getVer());
    }
}
