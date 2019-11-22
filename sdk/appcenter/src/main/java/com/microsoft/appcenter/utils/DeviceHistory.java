/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import com.microsoft.appcenter.ingestion.models.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Model class that correlates Device to a crash at app relaunch.
 */
public class DeviceHistory {

    private static String KEY_TIMESTAMP = "timestamp";
    private static String KEY_DEVICE = "device";
    private long mTimestamp;
    private Device mDevice;

    DeviceHistory(long timestamp, Device device) {
        mTimestamp = timestamp;
        mDevice = device;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public Device getDevice() {
        return mDevice;
    }

    public static NavigableMap<Long, DeviceHistory> readDevicesHistory(JSONArray arrayObject) throws JSONException {
        NavigableMap<Long, DeviceHistory> devicesHistory = new TreeMap<>();
        for (int i = 0; i < arrayObject.length(); i++) {
            JSONObject deviceHelperObj = new JSONObject(arrayObject.get(i).toString());
            long timestamp = deviceHelperObj.getLong(DeviceHistory.KEY_TIMESTAMP);
            Device device = new Device();
            device.read(new JSONObject(deviceHelperObj.get(DeviceHistory.KEY_DEVICE).toString()));
            devicesHistory.put(timestamp, new DeviceHistory(timestamp, device));
        }
        return  devicesHistory;
    }

    public static JSONStringer writeDevicesHistory(JSONStringer writer, DeviceHistory deviceHistory) throws JSONException {
        writer.object();
        writer.key(DeviceHistory.KEY_TIMESTAMP).value(deviceHistory.getTimestamp());
        JSONStringer deviceWriter = new JSONStringer();
        Device device = deviceHistory.getDevice();
        deviceWriter.object();
        device.write(deviceWriter);
        deviceWriter.endObject();
        writer.key(DeviceHistory.KEY_DEVICE).value(deviceWriter);
        writer.endObject();
        return writer;
    }
}
