package com.microsoft.appcenter.analytics.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Event log.
 */
public class CommonSchemaEventLog extends CommonSchemaLog {

    public static final String TYPE = "commonSchemaEvent";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
    }
}
