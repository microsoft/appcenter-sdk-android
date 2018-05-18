package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

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
    public void read(JSONObject object) throws JSONException {
        setId(object.getString(ID));
        setVer(object.getString(VER));
        setLocale(object.getString(LOCALE));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(ID).value(getId());
        writer.key(VER).value(getVer());
        writer.key(LOCALE).value(getLocale());
    }
}
