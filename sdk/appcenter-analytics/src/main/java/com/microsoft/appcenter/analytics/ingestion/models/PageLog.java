package com.microsoft.appcenter.analytics.ingestion.models;

import com.microsoft.appcenter.ingestion.models.LogWithProperties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import static com.microsoft.appcenter.ingestion.models.CommonProperties.NAME;

/**
 * Page log.
 */
public class PageLog extends LogWithProperties {

    public static final String TYPE = "page";

    /**
     * Name of the page.
     */
    private String name;

    @Override
    public String getType() {
        return TYPE;
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

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setName(object.getString(NAME));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(NAME).value(getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PageLog pageLog = (PageLog) o;
        return name != null ? name.equals(pageLog.name) : pageLog.name == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
