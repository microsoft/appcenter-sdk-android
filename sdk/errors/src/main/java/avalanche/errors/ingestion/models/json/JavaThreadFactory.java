package avalanche.errors.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.json.ModelFactory;
import avalanche.errors.ingestion.models.JavaThread;

public class JavaThreadFactory implements ModelFactory<JavaThread> {

    private static final JavaThreadFactory sInstance = new JavaThreadFactory();

    private JavaThreadFactory() {
    }

    public static JavaThreadFactory getInstance() {
        return sInstance;
    }

    @Override
    public JavaThread create() {
        return new JavaThread();
    }

    @Override
    public List<JavaThread> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
