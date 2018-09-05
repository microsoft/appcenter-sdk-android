package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * The “device” extension tracks common device elements that are not available in the core envelope.
 */
public class DeviceExtension implements Model {

    /**
     * ID property.
     */
    private static final String ID = "id";

    /**
     * Device ID.
     */
    private String id;

    /**
     * Local ID property.
     */
    private static final String LOCAL_ID = "localId";

    /**
     * Local ID.
     */
    private String localId;

    /**
     * Auth ID property.
     */
    private static final String AUTH_ID = "authId";

    /**
     * Auth ID.
     */
    private String authId;

    /**
     * Auth sec ID property.
     */
    private static final String AUTH_SEC_ID = "authSecId";

    /**
     * Auth sec ID.
     */
    private String authSecId;

    /**
     * Get ID.
     *
     * @return device ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Set ID.
     *
     * @param id device ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get local ID.
     *
     * @return local ID.
     */
    public String getLocalId() {
        return localId;
    }

    /**
     * Set local ID.
     *
     * @param localId local ID.
     */
    public void setLocalId(String localId) {
        this.localId = localId;
    }

    /**
     * Get auth ID.
     *
     * @return auth ID.
     */
    public String getAuthId() {
        return authId;
    }

    /**
     * Set auth ID.
     *
     * @param authId auth ID.
     */
    public void setAuthId(String authId) {
        this.authId = authId;
    }

    /**
     * Get auth sec ID.
     *
     * @return auth sec ID.
     */
    public String getAuthSecId() {
        return authSecId;
    }

    /**
     * Set auth sec ID.
     *
     * @param authSecId auth sec ID.
     */
    public void setAuthSecId(String authSecId) {
        this.authSecId = authSecId;
    }

    @Override
    public void read(JSONObject object) {
        setId(object.optString(ID, null));
        setLocalId(object.optString(LOCAL_ID, null));
        setAuthId(object.optString(AUTH_ID, null));
        setAuthSecId(object.optString(AUTH_SEC_ID, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, LOCAL_ID, getLocalId());
        JSONUtils.write(writer, AUTH_ID, getAuthId());
        JSONUtils.write(writer, AUTH_SEC_ID, getAuthSecId());
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceExtension that = (DeviceExtension) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (localId != null ? !localId.equals(that.localId) : that.localId != null) return false;
        if (authId != null ? !authId.equals(that.authId) : that.authId != null) return false;
        return authSecId != null ? authSecId.equals(that.authSecId) : that.authSecId == null);
    }

    @Override
    public int hashCode() {
        return locale != null ? locale.hashCode() : 0;
    }
}
