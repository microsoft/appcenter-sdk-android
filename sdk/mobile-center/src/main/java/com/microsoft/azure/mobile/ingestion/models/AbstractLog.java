package com.microsoft.azure.mobile.ingestion.models;


import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.ingestion.models.json.JSONDateUtils;
import com.microsoft.azure.mobile.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Date;
import java.util.UUID;

import static com.microsoft.azure.mobile.ingestion.models.CommonProperties.TYPE;

/**
 * The AbstractLog model.
 */
public abstract class AbstractLog implements Log {

    /**
     * Session identifier property.
     */
    @VisibleForTesting
    static final String SID = "sid";

    /**
     * device property.
     */
    @VisibleForTesting
    static final String DEVICE = "device";

    /**
     * timestamp property.
     */
    private static final String TIMESTAMP = "timestamp";

    /**
     * Log timestamp.
     */
    private Date timestamp;

    /**
     * The session identifier that was provided when the session was started.
     */
    private UUID sid;

    /**
     * Device characteristics associated to this log.
     */
    private Device device;

    @Override
    public Date getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    public UUID getSid() {
        return this.sid;
    }

    /**
     * Set the sid value.
     *
     * @param sid the sid value to set
     */
    public void setSid(UUID sid) {
        this.sid = sid;
    }

    /**
     * Get the device value.
     *
     * @return the device value
     */
    public Device getDevice() {
        return this.device;
    }

    /**
     * Set the device value.
     *
     * @param device the device value to set
     */
    public void setDevice(Device device) {
        this.device = device;
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, TYPE, getType());
        writer.key(TIMESTAMP).value(JSONDateUtils.toString(getTimestamp()));
        JSONUtils.write(writer, SID, getSid());
        if (getDevice() != null) {
            writer.key(DEVICE).object();
            getDevice().write(writer);
            writer.endObject();
        }
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        if (!object.getString(TYPE).equals(getType()))
            throw new JSONException("Invalid type");
        setTimestamp(JSONDateUtils.toDate(object.getString(TIMESTAMP)));
        if (object.has(SID)) {
            setSid(UUID.fromString(object.getString(SID)));
        }
        if (object.has(DEVICE)) {
            Device device = new Device();
            device.read(object.getJSONObject(DEVICE));
            setDevice(device);
        }
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractLog that = (AbstractLog) o;

        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null)
            return false;
        if (sid != null ? !sid.equals(that.sid) : that.sid != null) return false;
        return device != null ? device.equals(that.device) : that.device == null;
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (sid != null ? sid.hashCode() : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        return result;
    }
}
