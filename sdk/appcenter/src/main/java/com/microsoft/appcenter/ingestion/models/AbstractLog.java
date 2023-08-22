/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;


import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

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
    private static final String SID = "sid";

    /**
     * Distribution group ID property.
     */
    private static final String DISTRIBUTION_GROUP_ID = "distributionGroupId";

    /**
     * UserID property.
     */
    private static final String USER_ID = "userId";

    /**
     * device property.
     */
    @VisibleForTesting
    static final String DEVICE = "device";

    /**
     * Data residency region property.
     */
    @VisibleForTesting
    static final String DATA_RESIDENCY_REGION = "dataResidencyRegion";

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
     * The optional user identifier.
     */
    private String userId;

    /**
     * Device characteristics associated to this log.
     */
    private Device device;

    /**
     * Data residency region.
     */
    private @Nullable String dataResidencyRegion;

    /**
     * Transient object tag.
     */
    private Object tag;

    @Override
    public Date getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public UUID getSid() {
        return this.sid;
    }

    @Override
    public void setSid(UUID sid) {
        this.sid = sid;
    }

    @Override
    public String getDistributionGroupId() {
        return this.distributionGroupId;
    }

    @Override
    public void setDistributionGroupId(String distributionGroupId) {
        this.distributionGroupId = distributionGroupId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public Device getDevice() {
        return this.device;
    }

    @Override
    public void setDevice(Device device) {
        this.device = device;
    }

    @Nullable
    @Override
    public String getDataResidencyRegion() {
        return this.dataResidencyRegion;
    }

    @Override
    public void setDataResidencyRegion(@Nullable String dataResidencyRegion) {
        this.dataResidencyRegion = dataResidencyRegion;
    }

    @Override
    public Object getTag() {
        return tag;
    }

    @Override
    public void setTag(Object tag) {
        this.tag = tag;
    }

    @Override
    public synchronized void addTransmissionTarget(String transmissionTargetToken) {
        transmissionTargetTokens.add(transmissionTargetToken);
    }

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
        JSONUtils.write(writer, USER_ID, getUserId());
        if (getDevice() != null) {
            writer.key(DEVICE).object();
            getDevice().write(writer);
            writer.endObject();
        }
        if (getDataResidencyRegion() != null) {
            JSONUtils.write(writer, DATA_RESIDENCY_REGION, getDataResidencyRegion());
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
        setDistributionGroupId(object.optString(DISTRIBUTION_GROUP_ID, null));
        setUserId(object.optString(USER_ID, null));
        if (object.has(DEVICE)) {
            Device device = new Device();
            device.read(object.getJSONObject(DEVICE));
            setDevice(device);
        }
        if (object.has(DATA_RESIDENCY_REGION)) {
            setDataResidencyRegion(object.optString(DATA_RESIDENCY_REGION, null));
        }
    }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
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
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (device != null ? !device.equals(that.device) : that.device != null) return false;
        if (dataResidencyRegion != null ? !dataResidencyRegion.equals(that.dataResidencyRegion) : that.dataResidencyRegion != null) return false;
        return tag != null ? tag.equals(that.tag) : that.tag == null;
    }

    @Override
    public int hashCode() {
        int result = transmissionTargetTokens.hashCode();
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (sid != null ? sid.hashCode() : 0);
        result = 31 * result + (distributionGroupId != null ? distributionGroupId.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        result = 31 * result + (dataResidencyRegion != null ? dataResidencyRegion.hashCode() : 0);
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }
}
