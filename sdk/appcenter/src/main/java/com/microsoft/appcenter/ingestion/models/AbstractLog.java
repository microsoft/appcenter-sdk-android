package com.microsoft.appcenter.ingestion.models;


import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.TYPE;

/**
 * The AbstractLog model.
 */
public abstract class AbstractLog implements Log {

    /**
     * timestamp property.
     */
    @VisibleForTesting
    static final String TIMESTAMP = "timestamp";

    /**
     * Session identifier property.
     */
    @VisibleForTesting
    static final String SID = "sid";

    /**
     * Distribution group ID property.
     */
    @VisibleForTesting
    static final String DISTRIBUTION_GROUP_ID = "distributionGroupId";

    /**
     * device property.
     */
    @VisibleForTesting
    static final String DEVICE = "device";

    /**
     * Collection of transmissionTargetTokens that this log should be sent to.
     */
    private final Set<String> transmissionTargetTokens = new LinkedHashSet<>();

    /**
     * Log timestamp.
     */
    private Date timestamp;

    /**
     * The session identifier that was provided when the session was started.
     */
    private UUID sid;

    /**
     * Optional distribution group ID value.
     */
    private String distributionGroupId;

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
     * Get the distributionGroupId value.
     *
     * @return the distributionGroupId value.
     */
    public String getDistributionGroupId() {
        return this.distributionGroupId;
    }

    /**
     * Set the distributionGroupId value.
     *
     * @param distributionGroupId the distributionGroupId value to set.
     */
    public void setDistributionGroupId(String distributionGroupId) {
        this.distributionGroupId = distributionGroupId;
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

    /**
     * Adds a transmission target that this log should be sent to.
     *
     * @param transmissionTargetToken the identifier of the transmission target.
     */
    @Override
    public synchronized void addTransmissionTarget(String transmissionTargetToken) {
        transmissionTargetTokens.add(transmissionTargetToken);
    }

    /**
     * Gets all transmissionTargetTokens that this log should be sent to.
     *
     * @return Collection of transmissionTargetTokens that this log should be sent to.
     */
    @Override
    public synchronized Set<String> getTransmissionTargetTokens() {
        return Collections.unmodifiableSet(transmissionTargetTokens);
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, TYPE, getType());
        writer.key(TIMESTAMP).value(JSONDateUtils.toString(getTimestamp()));
        JSONUtils.write(writer, SID, getSid());
        JSONUtils.write(writer, DISTRIBUTION_GROUP_ID, getDistributionGroupId());
        if (getDevice() != null) {
            writer.key(DEVICE).object();
            getDevice().write(writer);
            writer.endObject();
        }
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        if (!object.getString(TYPE).equals(getType())) {
            throw new JSONException("Invalid type");
        }
        setTimestamp(JSONDateUtils.toDate(object.getString(TIMESTAMP)));
        if (object.has(SID)) {
            setSid(UUID.fromString(object.getString(SID)));
        }
        if (object.has(DISTRIBUTION_GROUP_ID)) {
            setDistributionGroupId(object.getString(DISTRIBUTION_GROUP_ID));
        }
        if (object.has(DEVICE)) {
            Device device = new Device();
            device.read(object.getJSONObject(DEVICE));
            setDevice(device);
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractLog that = (AbstractLog) o;

        if (!transmissionTargetTokens.equals(that.transmissionTargetTokens)) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null)
            return false;
        if (sid != null ? !sid.equals(that.sid) : that.sid != null) return false;
        if (distributionGroupId != null ? !distributionGroupId.equals(that.distributionGroupId) : that.distributionGroupId != null)
            return false;
        return device != null ? device.equals(that.device) : that.device == null;
    }

    @Override
    public int hashCode() {
        int result = transmissionTargetTokens.hashCode();
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (sid != null ? sid.hashCode() : 0);
        result = 31 * result + (distributionGroupId != null ? distributionGroupId.hashCode() : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        return result;
    }
}
