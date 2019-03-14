/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

import org.json.JSONException;

import java.util.Collection;

public interface LogSerializer {

    @NonNull
    String serializeLog(@NonNull Log log) throws JSONException;

    @NonNull
    Log deserializeLog(@NonNull String json, String type) throws JSONException;

    Collection<CommonSchemaLog> toCommonSchemaLog(@NonNull Log log);

    @NonNull
    String serializeContainer(@NonNull LogContainer container) throws JSONException;

    @NonNull
    LogContainer deserializeContainer(@NonNull String json, String type) throws JSONException;

    void addLogFactory(@NonNull String logType, @NonNull LogFactory logFactory);
}
