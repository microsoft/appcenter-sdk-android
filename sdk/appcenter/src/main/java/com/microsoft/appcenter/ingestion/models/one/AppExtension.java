/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
     * Name property.
     */
    private static final String NAME = "name";

    /**
     * Locale property.
     */
    private static final String LOCALE = "locale";

    /**
     * User ID.
     */
    private static final String USER_ID = "userId";

    /**
     * Application identifier.
     */
    private String id;

    /**
     * Application version.
     */
    private String ver;

    /**
     * Application name.
     */
    private String name;

    /**
     * Application locale.
     */
    private String locale;

    /**
     * User ID.
     */
    private String userId;

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
     * Get application name.
     *
     * @return application name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set application name.
     *
     * @param name application name.
     */
    public void setName(String name) {
        this.name = name;
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

    /**
     * Get user ID.
     *
     * @return user ID.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public String getUserId() {
        return userId;
    }

    /**
     * Set user ID.
     *
     * @param userId user ID.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void read(JSONObject object) {
        setId(object.optString(ID, null));
        setVer(object.optString(VER, null));
        setName(object.optString(NAME, null));
        setLocale(object.optString(LOCALE, null));
        setUserId(object.optString(USER_ID, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, VER, getVer());
        JSONUtils.write(writer, NAME, getName());
        JSONUtils.write(writer, LOCALE, getLocale());
        JSONUtils.write(writer, USER_ID, getUserId());
    }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppExtension that = (AppExtension) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (ver != null ? !ver.equals(that.ver) : that.ver != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (locale != null ? !locale.equals(that.locale) : that.locale != null) return false;
        return userId != null ? userId.equals(that.userId) : that.userId == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (ver != null ? ver.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        return result;
    }
}
