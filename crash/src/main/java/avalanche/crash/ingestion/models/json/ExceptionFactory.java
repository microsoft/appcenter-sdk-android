package avalanche.crash.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.base.ingestion.models.json.DefinitionFactory;
import avalanche.crash.ingestion.models.Exception;

public class ExceptionFactory implements DefinitionFactory<Exception> {

    private static ExceptionFactory sInstance = new ExceptionFactory();

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
