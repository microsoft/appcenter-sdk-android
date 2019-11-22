/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.ingestion.models.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Model class that correlates Device to a crash at app relaunch.
 */
public class DeviceHistory implements Comparator<DeviceHistory>, Comparable<DeviceHistory> {

    public static String KEY_TIMESTAMP = "mTimestamp";
    public static String KEY_DEVICE = "mDevice";

    private long mTimestamp;
    private Device mDevice;

    public DeviceHistory(long getTimestamp, Device getDevice) {
        mTimestamp = getTimestamp;
        mDevice = getDevice;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public Device getGetDevice() {
        return mDevice;
    }

    /**
     * todo
     * @param arrayObject
     * @return
     * @throws JSONException
     */
    public static SortedSet<DeviceHistory> readDevicesHistory(JSONArray arrayObject) throws JSONException {
        SortedSet<DeviceHistory> devicesHistory = new TreeSet<>();
        for (int i = 0; i < arrayObject.length(); i++) {
            JSONObject deviceHelperObj = new JSONObject(arrayObject.get(i).toString());
            long timestamp = deviceHelperObj.getLong(DeviceHistory.KEY_TIMESTAMP);
            Device device = new Device();
            device.read(deviceHelperObj.getJSONObject(DeviceHistory.KEY_DEVICE));
            devicesHistory.add(new DeviceHistory(timestamp, device));
        }
        return  devicesHistory;
    }

    /**
     * todo
     * @param writer
     * @param deviceHistory
     * @return
     * @throws JSONException
     */
    public static JSONStringer writeDevicesHistory(JSONStringer writer, DeviceHistory deviceHistory) throws JSONException {
        writer.object();
        writer.key(DeviceHistory.KEY_TIMESTAMP).value(deviceHistory.getTimestamp());
        JSONStringer deviceWriter = new JSONStringer();
        Device device = deviceHistory.getGetDevice();
        deviceWriter.object();
        device.write(deviceWriter);
        deviceWriter.endObject();
        writer.key(DeviceHistory.KEY_DEVICE).value(deviceWriter);
        writer.endObject();
        return writer;
    }

    @Override
    public int compareTo(@NonNull DeviceHistory o) {
        return (int)(getTimestamp() - o.getTimestamp());
    }

    @Override
    public int compare(DeviceHistory o1, DeviceHistory o2) {
        return (int)(o1.getTimestamp() - o2.getTimestamp());
    }
}
