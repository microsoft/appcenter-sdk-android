package avalanche.base.ingestion.models.utils;

import java.util.List;

import avalanche.base.ingestion.models.Model;

public class LogUtils {

    public static void checkNotNull(String key, Object value) {
        if (value == null)
            throw new IllegalArgumentException(key + " cannot be null");
    }

    public static void validateArray(List<? extends Model> models) {
        if (models != null)
            for (Model model : models)
                model.validate();
    }
}
