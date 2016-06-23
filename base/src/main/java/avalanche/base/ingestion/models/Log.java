package avalanche.base.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public interface Log {

    /**
     * type property
     */
    String TYPE = "type";

    /**
     * toffset property
     */
    String TOFFSET = "toffset";

    /**
     * Get the toffset value.
     *
     * @return the toffset value
     */
    long getToffset();

    /**
     * Set the toffset value.
     *
     * @param toffset the toffset value to set
     */
    void setToffset(long toffset);

    /**
     * Get the type value.
     *
     * @return the type value
     */
    String getType();

    void read(JSONObject object) throws JSONException;

    void write(JSONStringer writer) throws JSONException;

    void validate() throws IllegalArgumentException;
}
