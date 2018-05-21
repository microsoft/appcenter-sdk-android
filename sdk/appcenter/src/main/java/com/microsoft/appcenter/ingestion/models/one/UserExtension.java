package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * The “user” extension tracks common user elements that are not available in the core envelope.
 */
public class UserExtension implements Model {

    /**
     * Locale property.
     */
    private static final String LOCALE = "locale";

    /**
     * User locale.
     */
    private String locale;

    /**
     * Get user locale.
     *
     * @return user locale.
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Set user locale.
     *
     * @param locale user locale.
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setLocale(object.optString(LOCALE, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, LOCALE, getLocale());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserExtension that = (UserExtension) o;

        return locale != null ? locale.equals(that.locale) : that.locale == null;
    }

    @Override
    public int hashCode() {
        return locale != null ? locale.hashCode() : 0;
    }
}
