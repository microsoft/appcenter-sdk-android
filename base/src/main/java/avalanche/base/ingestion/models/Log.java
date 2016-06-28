package avalanche.base.ingestion.models;

public interface Log extends Model {

    /**
     * Get the type value.
     *
     * @return the type value
     */
    String getType();

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
}
