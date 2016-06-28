package avalanche.base.ingestion.models.utils;

public class LogUtils {

    public static void checkNotNull(String key, Object value) {
        if (value == null)
            throw new IllegalArgumentException(key + " cannot be null");
    }
}
