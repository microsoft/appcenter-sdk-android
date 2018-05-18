package com.microsoft.appcenter.ingestion.models.one;

import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.ingestion.models.AbstractLog;
import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Common schema has 1 log type with extensions, everything is called an event.
 * Part B can be used in the future for domain specific typing (like reflecting AppCenter log type).
 */
public abstract class CommonSchemaLog extends AbstractLog {

    /**
     * Common schema version property.
     */
    @VisibleForTesting
    static final String VER = "ver";

    /**
     * Name property.
     */
    @VisibleForTesting
    static final String NAME = "name";

    /**
     * Time property.
     */
    @VisibleForTesting
    static final String TIME = "time";

    /**
     * iKey property.
     */
    @VisibleForTesting
    static final String IKEY = "iKey";

    /**
     * Extensions property.
     */
    @VisibleForTesting
    static final String EXT = "ext";

    /**
     * Common schema version.
     */
    private String ver;

    /**
     * Event name.
     */
    private String name;

    /**
     * An identifier used to identify applications or other logical groupings of events.
     */
    private String iKey;

    /**
     * Part A Extensions.
     */
    private Extensions ext;

    /**
     * Get common schema version.
     *
     * @return common schema version.
     */
    public String getVer() {
        return ver;
    }

    /**
     * Set common schema version.
     *
     * @param ver common schema version.
     */
    public void setVer(String ver) {
        this.ver = ver;
    }

    /**
     * Get event name.
     *
     * @return event name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set event name.
     *
     * @param name event name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get instrumentation key.
     *
     * @return instrumentation key.
     */
    public String getIKey() {
        return iKey;
    }

    /**
     * Set instrumentation key.
     *
     * @param iKey instrumentation key.
     */
    public void setIKey(String iKey) {
        this.iKey = iKey;
    }

    /**
     * Get Part A extensions.
     *
     * @return Part A extensions.
     */
    public Extensions getExt() {
        return ext;
    }

    /**
     * Set Part A extensions.
     *
     * @param ext Part A extensions.
     */
    public void setExt(Extensions ext) {
        this.ext = ext;
    }

    @Override
    public void read(JSONObject object) throws JSONException {

        /* Override abstract log JSON since it's Common Schema and not App Center schema. */

        /* Read top level PART A simple fields. */
        setVer(object.getString(VER));
        setName(object.getString(NAME));
        setTimestamp(JSONDateUtils.toDate(object.getString(TIME)));
        setIKey(object.getString(IKEY));

        /* Read extensions. */
        Extensions extensions = new Extensions();
        extensions.read(object.getJSONObject(EXT));
        setExt(extensions);
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {

        /* Override abstract log JSON since it's Common Schema and not App Center schema. */

        /* Part A. */
        writer.key(VER).value(getVer());
        writer.key(NAME).value(getName());
        writer.key(TIME).value(JSONDateUtils.toString(getTimestamp()));
        writer.key(IKEY).value(getIKey());

        /* Part A extensions. */
        writer.key(EXT).object();
        getExt().write(writer);
        writer.endObject();
    }
}
