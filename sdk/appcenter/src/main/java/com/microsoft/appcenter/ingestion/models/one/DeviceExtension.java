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
 * The "device" extension tracks common device elements that are not available in the core envelope.
 */
public class DeviceExtension implements Model {

    /**
     * Local ID property.
     */
    private static final String LOCAL_ID = "localId";

    /**
     * Local ID.
     */
    private String localId;

    /**
     * Get local ID.
     *
     * @return local ID.
     */
    public String getLocalId() {
        return localId;
    }

    /**
     * Set local ID.
     *
     * @param localId local ID.
     */
    public void setLocalId(String localId) {
        this.localId = localId;
    }

    @Override
    public void read(JSONObject object) {
        setLocalId(object.optString(LOCAL_ID, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, LOCAL_ID, getLocalId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceExtension that = (DeviceExtension) o;

        return localId != null ? localId.equals(that.localId) : that.localId == null;
    }

    @Override
    public int hashCode() {
        return localId != null ? localId.hashCode() : 0;
    }
}
