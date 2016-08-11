package avalanche.errors.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.json.ModelFactory;
import avalanche.errors.ingestion.models.JavaException;

public class JavaExceptionFactory implements ModelFactory<JavaException> {

    private static final JavaExceptionFactory sInstance = new JavaExceptionFactory();

    private JavaExceptionFactory() {
    }

    public static JavaExceptionFactory getInstance() {
        return sInstance;
    }

    @Override
    public JavaException create() {
        return new JavaException();
    }

    @Override
    public List<JavaException> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
