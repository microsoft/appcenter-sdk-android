package avalanche.base.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import avalanche.base.ingestion.models.utils.LogUtils;

import static avalanche.base.ingestion.models.CommonProperties.SID;

/**
 * The InSessionLog model.
 */
public abstract class InSessionLog extends AbstractLog {

    private static final String PROPERTIES = "properties";

    /**
     * Additional key/value pair parameters.
     */
    private Map<String, String> properties;

    /**
     * The session identifier that was provided when the session was started.
     */
    private String sid;

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * Set the properties value.
     *
     * @param properties the properties value to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    public String getSid() {
        return this.sid;
    }

    /**
     * Set the sid value.
     *
     * @param sid the sid value to set
     */
    public void setSid(String sid) {
        this.sid = sid;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setSid(object.getString(SID));
        JSONObject jProperties = object.optJSONObject(PROPERTIES);
        if (jProperties != null) {
            Map<String, String> properties = new HashMap<>(jProperties.length());
            Iterator<String> keys = jProperties.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                properties.put(key, jProperties.getString(key));
            }
            setProperties(properties);
        }
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writer.key(SID).value(getSid());
        CommonProperties.serializeMap(PROPERTIES, getProperties(), writer);
    }

    @Override
    public void validate() throws IllegalArgumentException {
        super.validate();
        LogUtils.checkNotNull(SID, getSid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        InSessionLog that = (InSessionLog) o;

        if (properties != null ? !properties.equals(that.properties) : that.properties != null)
            return false;
        return sid != null ? sid.equals(that.sid) : that.sid == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (sid != null ? sid.hashCode() : 0);
        return result;
    }
}
