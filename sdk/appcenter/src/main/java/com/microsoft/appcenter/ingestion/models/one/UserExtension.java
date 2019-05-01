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
 * The "user" extension tracks common user elements that are not available in the core envelope.
 */
public class UserExtension implements Model {

    /**
     * LocalId property.
     */
    private static final String LOCAL_ID = "localId";

    /**
     * Locale property.
     */
    private static final String LOCALE = "locale";

    /**
     * Local Id.
     */
    private String localId;

    /**
     * User locale.
     */
    private String locale;

    /**
     * Get localId.
     *
     * @return localId.
     */
    @SuppressWarnings("WeakerAccess")
    public String getLocalId() {
        return localId;
    }

    /**
     * set localId.
     *
     * @param localId localId.
     */
    @SuppressWarnings("WeakerAccess")
    public void setLocalId(String localId) {
        this.localId = localId;
    }

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
    public void read(JSONObject object) {
        setLocalId(object.optString(LOCAL_ID, null));
        setLocale(object.optString(LOCALE, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, LOCAL_ID, getLocalId());
        JSONUtils.write(writer, LOCALE, getLocale());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserExtension that = (UserExtension) o;

        if (localId != null ? !localId.equals(that.localId) : that.localId != null) return false;
        return locale != null ? locale.equals(that.locale) : that.locale == null;
    }

    @Override
    public int hashCode() {
        int result = localId != null ? localId.hashCode() : 0;
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        return result;
    }
}
