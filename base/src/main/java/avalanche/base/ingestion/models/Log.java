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

    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    String getSid();

    /**
     * Set the sid value.
     *
     * @param sid the sid value to set
     */
    void setSid(String sid);
}
