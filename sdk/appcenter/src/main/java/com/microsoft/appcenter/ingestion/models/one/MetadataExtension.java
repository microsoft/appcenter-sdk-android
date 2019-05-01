/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Iterator;

/**
 * Part A extension for metadata of Part B and Part C fields.
 */
public class MetadataExtension implements Model {

    /**
     * Metadata.
     */
    private JSONObject mMetadata = new JSONObject();

    /**
     * Get metadata.
     *
     * @return metadata.
     */
    public JSONObject getMetadata() {
        return mMetadata;
    }

    @Override
    public void read(JSONObject object) {
        mMetadata = object;
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        for (Iterator<String> iterator = mMetadata.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            writer.key(key).value(mMetadata.get(key));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataExtension metadataExtension = (MetadataExtension) o;

        return mMetadata.toString().equals(metadataExtension.mMetadata.toString());
    }

    @Override
    public int hashCode() {
        return mMetadata.toString().hashCode();
    }
}
