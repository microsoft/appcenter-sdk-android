/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models;

import com.microsoft.appcenter.crashes.ingestion.models.json.StackFrameFactory;
import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.FRAMES;
import static com.microsoft.appcenter.ingestion.models.CommonProperties.ID;
import static com.microsoft.appcenter.ingestion.models.CommonProperties.NAME;

/**
 * The Thread model.
 */
public class Thread implements Model {

    /**
     * Thread identifier.
     */
    private long id;

    /**
     * Thread name.
     */
    private String name;

    /**
     * Stack frames.
     */
    private List<StackFrame> frames;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public long getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the frames value.
     *
     * @return the frames value
     */
    public List<StackFrame> getFrames() {
        return this.frames;
    }

    /**
     * Set the frames value.
     *
     * @param frames the frames value to set
     */
    public void setFrames(List<StackFrame> frames) {
        this.frames = frames;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setId(object.getLong(ID));
        setName(object.optString(NAME, null));
        setFrames(JSONUtils.readArray(object, FRAMES, StackFrameFactory.getInstance()));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, NAME, getName());
        JSONUtils.writeArray(writer, FRAMES, getFrames());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Thread that = (Thread) o;
        if (id != that.id) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return frames != null ? frames.equals(that.frames) : that.frames == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (frames != null ? frames.hashCode() : 0);
        return result;
    }
}
