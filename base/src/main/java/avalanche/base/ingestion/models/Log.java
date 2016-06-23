package avalanche.base.ingestion.models;


/**
 * The Log model.
 */
public abstract class Log {

    /**
     * Corresponds to the number of milliseconds elapsed between the time the
     * request is sent and the time the log is emitted.
     */
    private long toffset;

    /**
     * Get the toffset value.
     *
     * @return the toffset value
     */
    public long getToffset() {
        return this.toffset;
    }

    /**
     * Set the toffset value.
     *
     * @param toffset the toffset value to set
     */
    public void setToffset(long toffset) {
        this.toffset = toffset;
    }
}
