package avalanche.errors.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Map;

import avalanche.core.ingestion.models.Model;
import avalanche.core.ingestion.models.json.JSONUtils;

/**
 * The ThreadFrame model.
 */
public class ThreadFrame implements Model {

    private static final String ADDRESS = "address";

    private static final String SYMBOL = "symbol";

    private static final String REGISTERS = "registers";

    /**
     * Frame address.
     */
    private String address;

    /**
     * Frame symbol.
     */
    private String symbol;

    /**
     * Registers.
     */
    private Map<String, String> registers;

    /**
     * Get the address value.
     *
     * @return the address value
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * Set the address value.
     *
     * @param address the address value to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Get the symbol value.
     *
     * @return the symbol value
     */
    public String getSymbol() {
        return this.symbol;
    }

    /**
     * Set the symbol value.
     *
     * @param symbol the symbol value to set
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Get the registers value.
     *
     * @return the registers value
     */
    public Map<String, String> getRegisters() {
        return this.registers;
    }

    /**
     * Set the registers value.
     *
     * @param registers the registers value to set
     */
    public void setRegisters(Map<String, String> registers) {
        this.registers = registers;
    }


    @Override
    public void read(JSONObject object) throws JSONException {
        setAddress(object.getString(ADDRESS));
        setAddress(object.optString(SYMBOL, null));
        setRegisters(JSONUtils.readMap(object, REGISTERS));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        JSONUtils.write(writer, ADDRESS, getAddress(), true);
        JSONUtils.write(writer, SYMBOL, getSymbol(), false);
        JSONUtils.writeMap(writer, REGISTERS, getRegisters());
    }

    @Override
    public void validate() throws IllegalArgumentException {
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreadFrame that = (ThreadFrame) o;

        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;
        return registers != null ? registers.equals(that.registers) : that.registers == null;

    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (registers != null ? registers.hashCode() : 0);
        return result;
    }
}
