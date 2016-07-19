package avalanche.errors.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.json.ModelFactory;
import avalanche.errors.ingestion.models.Exception;

public class ExceptionFactory implements ModelFactory<Exception> {

    private static final ExceptionFactory sInstance = new ExceptionFactory();

    private ExceptionFactory() {
    }

    public static ExceptionFactory getInstance() {
        return sInstance;
    }

    @Override
    public Exception create() {
        return new Exception();
    }

    @Override
    public List<Exception> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
