package avalanche.errors.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.json.ModelFactory;
import avalanche.errors.ingestion.models.JavaStackFrame;

public class JavaStackFrameFactory implements ModelFactory<JavaStackFrame> {

    private static final JavaStackFrameFactory sInstance = new JavaStackFrameFactory();

    private JavaStackFrameFactory() {
    }

    public static JavaStackFrameFactory getInstance() {
        return sInstance;
    }

    @Override
    public JavaStackFrame create() {
        return new JavaStackFrame();
    }

    @Override
    public List<JavaStackFrame> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
