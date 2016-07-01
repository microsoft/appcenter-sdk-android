package avalanche.base.ingestion;

public interface ServiceCall {

    /**
     * Cancel the call if possible.
     */
    void cancel();
}
