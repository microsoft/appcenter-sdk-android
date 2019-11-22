/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.utils.DeviceHistory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPE;

public class DefaultLogSerializer implements LogSerializer {

    private static final String LOGS = "logs";

    private final Map<String, LogFactory> mLogFactories = new HashMap<>();

    @NonNull
    private JSONStringer writeLog(JSONStringer writer, Log log) throws JSONException {
        writer.object();
        log.write(writer);
        writer.endObject();
        return writer;
    }

    @NonNull
    private JSONStringer writeDevices(JSONStringer writer, DeviceHistory deviceHistory) throws JSONException {
        return DeviceHistory.writeDevicesHistory(writer, deviceHistory);
    }

    @NonNull
    private Log readLog(JSONObject object, String type) throws JSONException {
        if (type == null) {
            type = object.getString(TYPE);
        }
        LogFactory logFactory = mLogFactories.get(type);
        if (logFactory == null) {
            throw new JSONException("Unknown log type: " + type);
        }
        Log log = logFactory.create();
        log.read(object);
        return log;
    }

    @NonNull
    private SortedSet<DeviceHistory> readDevices(JSONArray arrayObject) throws JSONException {
        return DeviceHistory.readDevicesHistory(arrayObject);
    }

    @NonNull
    @Override
    public String serializeLog(@NonNull Log log) throws JSONException {
        return writeLog(new JSONStringer(), log).toString();
    }

    @NonNull
    @Override
    public Log deserializeLog(@NonNull String json, String type) throws JSONException {
        return readLog(new JSONObject(json), type);
    }

    @NonNull
    @Override
    public Set<String> serializeDevices(@NonNull SortedSet<DeviceHistory> device) throws JSONException {
        Set<String> deviceHistories = new HashSet<>();
        for (DeviceHistory deviceHistory : device) {
            deviceHistories.add(writeDevices(new JSONStringer(), deviceHistory).toString());
        }
        return deviceHistories;
    }

    @NonNull
    @Override
    public SortedSet<DeviceHistory> deserializeDevices(@NonNull Set<String> json) throws JSONException {
        return readDevices(new JSONArray(json));
    }

    @Override
    public Collection<CommonSchemaLog> toCommonSchemaLog(@NonNull Log log) {
        return mLogFactories.get(log.getType()).toCommonSchemaLogs(log);
    }

    @NonNull
    @Override
    public String serializeContainer(@NonNull LogContainer logContainer) throws JSONException {

        /* Init JSON serializer. */
        JSONStringer writer = new JSONStringer();

        /* Start writing JSON. */
        writer.object();
        writer.key(LOGS).array();
        for (Log log : logContainer.getLogs()) {
            writeLog(writer, log);
        }
        writer.endArray();
        writer.endObject();
        return writer.toString();
    }

    @NonNull
    @Override
    public LogContainer deserializeContainer(@NonNull String json, String type) throws JSONException {
        JSONObject jContainer = new JSONObject(json);
        LogContainer container = new LogContainer();
        JSONArray jLogs = jContainer.getJSONArray(LOGS);
        List<Log> logs = new ArrayList<>();
        for (int i = 0; i < jLogs.length(); i++) {
            JSONObject jLog = jLogs.getJSONObject(i);
            Log log = readLog(jLog, type);
            logs.add(log);
        }
        container.setLogs(logs);
        return container;
    }

    @Override
    public void addLogFactory(@NonNull String logType, @NonNull LogFactory logFactory) {
        mLogFactories.put(logType, logFactory);
    }
}
