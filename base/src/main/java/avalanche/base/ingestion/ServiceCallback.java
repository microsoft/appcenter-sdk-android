package avalanche.base.ingestion;

/**
 * The callback used for client side asynchronous operations.
 *
 * @param <T> the type of the response
 */
public abstract class ServiceCallback<T> {

    /**
     * Override this method to handle REST call failures.
     *
     * @param t the exception thrown from the pipeline.
     */
    public abstract void failure(Throwable t);

    /**
     * Override this method to handle successful REST call results.
     *
     * @param result the ServiceResponse holding the response.
     */
    public abstract void success(T result);
}