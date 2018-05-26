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
     * Part B data type.
     */
    private String baseDataType;

    /**
     * Part C properties.
     */
    private JSONObject mProperties = new JSONObject();

    /**
     * Get base data type.
     *
     * @return base data type.
     */
    @SuppressWarnings("WeakerAccess")
    public String getBaseDataType() {
        return baseDataType;
    }

    /**
     * Set base data type.
     *
     * @param baseDataType base data type.
     */
    public void setBaseDataType(String baseDataType) {
        this.baseDataType = baseDataType;
    }

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

        /* Part B. We don't really handle it yet but we need a type internally. */
        setBaseDataType(object.getString(BASE_DATA_TYPE));

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

        /* Part B. We don't really handle it yet but we need a type internally. */
        writer.key(BASE_DATA_TYPE).value(getBaseDataType());

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

        if (baseDataType != null ? !baseDataType.equals(data.baseDataType) : data.baseDataType != null)
            return false;
        return mProperties.toString().equals(data.mProperties.toString());
    }

    @Override
    public int hashCode() {
        int result = baseDataType != null ? baseDataType.hashCode() : 0;
        result = 31 * result + mProperties.toString().hashCode();
        return result;
    }
}
