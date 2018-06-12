package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * This is the application extension. It contains data specified by the application.
 */
public class AppExtension implements Model {

    /**
     * Id property.
     */
    private static final String ID = "id";

    /**
     * Version property.
     */
    private static final String VER = "ver";

    /**
     * Locale property.
     */
    private static final String LOCALE = "locale";

    /**
     * Application identifier.
     */
    private String id;

    /**
     * Application version.
     */
    private String ver;

    /**
     * Application locale.
     */
    private String locale;

    /**
     * Get application id.
     *
     * @return application id.
     */
    public String getId() {
        return id;
    }

    /**
     * Set application id.
     *
     * @param id application id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get application version.
     *
     * @return application version.
     */
    public String getVer() {
        return ver;
    }

    /**
     * Set application version.
     *
     * @param ver application version.
     */
    public void setVer(String ver) {
        this.ver = ver;
    }

    /**
     * Get application locale.
     *
     * @return application locale.
     */
    @SuppressWarnings("WeakerAccess")
    public String getLocale() {
        return locale;
    }

    /**
     * Set application locale.
     *
     * @param locale application locale.
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public void read(JSONObject object) {
        setId(object.optString(ID, null));
        setVer(object.optString(VER, null));
        setLocale(object.optString(LOCALE, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, VER, getVer());
        JSONUtils.write(writer, LOCALE, getLocale());
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppExtension that = (AppExtension) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (ver != null ? !ver.equals(that.ver) : that.ver != null) return false;
        return locale != null ? locale.equals(that.locale) : that.locale == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (ver != null ? ver.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        return result;
    }
}
