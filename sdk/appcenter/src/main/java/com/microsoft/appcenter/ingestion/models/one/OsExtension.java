package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * The “os” extension tracks common os elements that are not available in the core envelope.
 */
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
    public void read(JSONObject object) {
        setName(object.optString(NAME, null));
        setVer(object.optString(VER, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, NAME, getName());
        JSONUtils.write(writer, VER, getVer());
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OsExtension that = (OsExtension) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return ver != null ? ver.equals(that.ver) : that.ver == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (ver != null ? ver.hashCode() : 0);
        return result;
    }
}
