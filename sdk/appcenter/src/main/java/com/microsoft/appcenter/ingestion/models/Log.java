package com.microsoft.appcenter.ingestion.models;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

public interface Log extends Model {

    /**
     * Get the type value.
     *
     * @return the type value
     */
    String getType();

    /**
     * Get the timestamp value.
     *
     * @return the timestamp value
     */
    Date getTimestamp();

    /**
     * Set the timestamp value.
     *
     * @param timestamp the timestamp value to set
     */
    void setTimestamp(Date timestamp);

    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    UUID getSid();

    /**
     * Set the sid value.
     *
     * @param sid the sid value to set
     */
    void setSid(UUID sid);

    /**
     * Get the distributionGroupId value.
     *
     * @return the distributionGroupId value.
     */
    String getDistributionGroupId();

    /**
     * Set the distributionGroupId value.
     *
     * @param distributionGroupId the distributionGroupId value to set.
     */
    void setDistributionGroupId(String distributionGroupId);

    /**
     * Get the device value.
     *
     * @return the device value
     */
    @SuppressWarnings("unused")
    Device getDevice();

    /**
     * Set the device value.
     *
     * @param device the device value to set
     */
    void setDevice(Device device);

    /**
     * Adds a transmissionTargetToken that this log should be sent to.
     *
     * @param transmissionTargetToken the identifier of the transmissionTargetToken.
     */
    @SuppressWarnings("unused")
    void addTransmissionTarget(String transmissionTargetToken);

    /**
     * Gets all transmission targets that this log should be sent to.
     *
     * @return Collection of transmission targets that this log should be sent to.
     */
    @SuppressWarnings("unused")
    Set<String> getTransmissionTargetTokens();
}
