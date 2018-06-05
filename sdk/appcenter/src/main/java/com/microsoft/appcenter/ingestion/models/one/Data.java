package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Object that contains Part B and Part C from Common Schema.
 */
public class Data implements Model {

    /**
     * Part B data type property.
     */
    public static final String BASE_DATA_TYPE = "baseDataType";

    /**
     * Part B data property.
     */
    public static final String BASE_DATA = "baseData";

    /**
     * Part C properties.
     */
    private JSONObject mProperties = new JSONObject();

    /**
     * Get Part C properties.
     *
     * @return properties.
     */
    public JSONObject getProperties() {
        return mProperties;
    }

    @Override
    public void read(JSONObject object) throws JSONException {

        /* Part C. */
        JSONArray names = object.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals(BASE_DATA) && !name.equals(BASE_DATA_TYPE)) {
                    mProperties.put(name, object.get(name));
                }
            }
        }
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {

        /* Part C. */
        JSONArray names = mProperties.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                writer.key(name).value(mProperties.get(name));
            }
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data data = (Data) o;

        return mProperties.toString().equals(data.mProperties.toString());
    }

    @Override
    public int hashCode() {
        return mProperties.toString().hashCode();
    }
}
