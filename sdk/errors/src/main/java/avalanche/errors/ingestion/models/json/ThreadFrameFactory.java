package avalanche.errors.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.json.ModelFactory;
import avalanche.errors.ingestion.models.ThreadFrame;

public class ThreadFrameFactory implements ModelFactory<ThreadFrame> {

    private static final ThreadFrameFactory sInstance = new ThreadFrameFactory();

    private ThreadFrameFactory() {
    }

    public static ThreadFrameFactory getInstance() {
        return sInstance;
    }

    @Override
    public ThreadFrame create() {
        return new ThreadFrame();
    }

    @Override
    public List<ThreadFrame> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
