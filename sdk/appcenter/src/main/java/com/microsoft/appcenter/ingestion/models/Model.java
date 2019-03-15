/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public interface Model {

    void read(JSONObject object) throws JSONException;

    void write(JSONStringer writer) throws JSONException;
}
