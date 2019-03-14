/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

/**
 * Extension for device specific information.
 */
public class ProtocolExtension implements Model {

    /**
     * TicketKeys property.
     */
    private static final String TICKET_KEYS = "ticketKeys";

    /**
     * Device manufacturer property.
     */
    private static final String DEV_MAKE = "devMake";

    /**
     * Device model property.
     */
    private static final String DEV_MODEL = "devModel";

    /**
     * Ticket keys.
     */
    private List<String> ticketKeys;

    /**
     * Device manufacturer.
     */
    private String devMake;

    /**
     * Device model.
     */
    private String devModel;

    /**
     * Get the ticket keys.
     *
     * @return ticket keys.
     */
    public List<String> getTicketKeys() {
        return ticketKeys;
    }

    /**
     * Set ticket keys.
     *
     * @param ticketKeys ticket keys.
     */
    public void setTicketKeys(List<String> ticketKeys) {
        this.ticketKeys = ticketKeys;
    }

    /**
     * Get device manufacturer.
     *
     * @return device manufacturer.
     */
    public String getDevMake() {
        return devMake;
    }

    /**
     * Set device manufacturer.
     *
     * @param devMake device manufacturer.
     */
    public void setDevMake(String devMake) {
        this.devMake = devMake;
    }

    /**
     * Get device model.
     *
     * @return device model.
     */
    public String getDevModel() {
        return devModel;
    }

    /**
     * Set device model.
     *
     * @param devModel device model.
     */
    public void setDevModel(String devModel) {
        this.devModel = devModel;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setTicketKeys(JSONUtils.readStringArray(object, TICKET_KEYS));
        setDevMake(object.optString(DEV_MAKE, null));
        setDevModel(object.optString(DEV_MODEL, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.writeStringArray(writer, TICKET_KEYS, getTicketKeys());
        JSONUtils.write(writer, DEV_MAKE, getDevMake());
        JSONUtils.write(writer, DEV_MODEL, getDevModel());
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProtocolExtension that = (ProtocolExtension) o;

        if (ticketKeys != null ? !ticketKeys.equals(that.ticketKeys) : that.ticketKeys != null)
            return false;
        if (devMake != null ? !devMake.equals(that.devMake) : that.devMake != null) return false;
        return devModel != null ? devModel.equals(that.devModel) : that.devModel == null;
    }

    @Override
    public int hashCode() {
        int result = ticketKeys != null ? ticketKeys.hashCode() : 0;
        result = 31 * result + (devMake != null ? devMake.hashCode() : 0);
        result = 31 * result + (devModel != null ? devModel.hashCode() : 0);
        return result;
    }
}
