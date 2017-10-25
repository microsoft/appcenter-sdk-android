package com.microsoft.appcenter.ingestion.models.json;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;

import org.json.JSONException;

public interface LogSerializer {

    @NonNull
    String serializeLog(@NonNull Log log) throws JSONException;

    @NonNull
    Log deserializeLog(@NonNull String json) throws JSONException;

    @NonNull
    String serializeContainer(@NonNull LogContainer container) throws JSONException;

    @NonNull
    LogContainer deserializeContainer(@NonNull String json) throws JSONException;

    void addLogFactory(@NonNull String logType, @NonNull LogFactory logFactory);
}
