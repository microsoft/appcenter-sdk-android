/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Object that contains Part B and Part C from Common Schema.
 */
public class Data implements Model {

    /**
     * Part B base type property.
     */
    static final String BASE_TYPE = "baseType";

    /**
     * Part B base data property.
     */
    static final String BASE_DATA = "baseData";

    /**
     * Part C properties.
     */
    private final JSONObject mProperties = new JSONObject();

    /**
     * Get Part C properties.
     *
     * @return properties.
     */
    public JSONObject getProperties() {
        return mProperties;
    }

    @Override
    public void read(JSONObject object) throws JSONException {

        /* Part B and C. */
        JSONArray names = object.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                mProperties.put(name, object.get(name));
            }
        }
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {

        /* Serialize part B before. */
        JSONUtils.write(writer, BASE_TYPE, mProperties.optString(BASE_TYPE, null));
        JSONUtils.write(writer, BASE_DATA, mProperties.optJSONObject(BASE_DATA));

        /* Then part C. */
        JSONArray names = mProperties.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals(BASE_TYPE) && !name.equals(BASE_DATA)) {
                    writer.key(name).value(mProperties.get(name));
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data data = (Data) o;

        return mProperties.toString().equals(data.mProperties.toString());
    }

    @Override
    public int hashCode() {
        return mProperties.toString().hashCode();
    }
}
