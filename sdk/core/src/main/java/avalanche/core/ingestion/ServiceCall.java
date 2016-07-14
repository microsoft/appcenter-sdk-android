package avalanche.core.ingestion;

public interface ServiceCall {

    /**
     * Cancel the call if possible.
     */
    void cancel();
}
