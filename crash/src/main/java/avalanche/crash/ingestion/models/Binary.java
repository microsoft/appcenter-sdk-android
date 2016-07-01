package avalanche.crash.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import avalanche.base.ingestion.models.Model;
import avalanche.base.ingestion.models.utils.LogUtils;

/**
 * The Binary model.
 */
public class Binary implements Model {

    private static final String START_ADDRESS = "startAddress";

    private static final String END_ADDRESS = "endAddress";

    private static final String NAME = "name";

    private static final String ARCHITECTURE = "architecture";

    private static final String UUID = "uuid";

    private static final String PATH = "path";

    /**
     * The startAddress property.
     */
    private String startAddress;

    /**
     * The endAddress property.
     */
    private String endAddress;

    /**
     * The name property.
     */
    private String name;

    /**
     * amd64, arm64, x86...
     */
    private String architecture;

    /**
     * The uuid property.
     */
    private String uuid;

    /**
     * The path property.
     */
    private String path;

    /**
     * Get the startAddress value.
     *
     * @return the startAddress value
     */
    public String getStartAddress() {
        return this.startAddress;
    }

    /**
     * Set the startAddress value.
     *
     * @param startAddress the startAddress value to set
     */
    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    /**
     * Get the endAddress value.
     *
     * @return the endAddress value
     */
    public String getEndAddress() {
        return this.endAddress;
    }

    /**
     * Set the endAddress value.
     *
     * @param endAddress the endAddress value to set
     */
    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
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
     * Get the architecture value.
     *
     * @return the architecture value
     */
    public String getArchitecture() {
        return this.architecture;
    }

    /**
     * Set the architecture value.
     *
     * @param architecture the architecture value to set
     */
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    /**
     * Get the uuid value.
     *
     * @return the uuid value
     */
    public String getUuid() {
        return this.uuid;
    }

    /**
     * Set the uuid value.
     *
     * @param uuid the uuid value to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Get the path value.
     *
     * @return the path value
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Set the path value.
     *
     * @param path the path value to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setStartAddress(object.getString(START_ADDRESS));
        setEndAddress(object.getString(END_ADDRESS));
        setName(object.getString(NAME));
        setArchitecture(object.getString(ARCHITECTURE));
        setUuid(object.getString(UUID));
        setPath(object.getString(PATH));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(START_ADDRESS).value(getStartAddress());
        writer.key(END_ADDRESS).value(getEndAddress());
        writer.key(NAME).value(getName());
        writer.key(ARCHITECTURE).value(getArchitecture());
        writer.key(UUID).value(getUuid());
        writer.key(PATH).value(getPath());
    }

    @Override
    public void validate() throws IllegalArgumentException {
        LogUtils.checkNotNull(START_ADDRESS, getStartAddress());
        LogUtils.checkNotNull(END_ADDRESS, getEndAddress());
        LogUtils.checkNotNull(NAME, getName());
        LogUtils.checkNotNull(ARCHITECTURE, getArchitecture());
        LogUtils.checkNotNull(UUID, getUuid());
        LogUtils.checkNotNull(PATH, getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Binary binary = (Binary) o;

        if (startAddress != null ? !startAddress.equals(binary.startAddress) : binary.startAddress != null)
            return false;
        if (endAddress != null ? !endAddress.equals(binary.endAddress) : binary.endAddress != null)
            return false;
        if (name != null ? !name.equals(binary.name) : binary.name != null) return false;
        if (architecture != null ? !architecture.equals(binary.architecture) : binary.architecture != null)
            return false;
        if (uuid != null ? !uuid.equals(binary.uuid) : binary.uuid != null) return false;
        return path != null ? path.equals(binary.path) : binary.path == null;
    }

    @Override
    public int hashCode() {
        int result = startAddress != null ? startAddress.hashCode() : 0;
        result = 31 * result + (endAddress != null ? endAddress.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (architecture != null ? architecture.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}
