package avalanche.core.utils;

/**
 * Abstract calls to system times for better testability.
 */
public interface TimeSource {

    /**
     * Returns the current time in milliseconds since Unix epoch, UTC.
     *
     * @return current time in milliseconds.
     */
    long currentTimeMillis();

    /**
     * Returns milliseconds since boot, including time spent in sleep.
     *
     * @return elapsed milliseconds since boot.
     */
    long elapsedRealtime();
}