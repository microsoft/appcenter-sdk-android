package avalanche.base.ingestion;

/**
 * The callback used for client side asynchronous operations.
 */
public interface ServiceCallback {

    /**
     * Implement this method to handle successful REST call results.
     */
    void success();

    /**
     * Implement this method to handle REST call failures.
     *
     * @param t the exception thrown from the pipeline.
     */
    void failure(Throwable t);
}