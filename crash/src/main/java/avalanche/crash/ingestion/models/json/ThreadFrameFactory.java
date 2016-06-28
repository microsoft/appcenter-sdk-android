package avalanche.crash.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.base.ingestion.models.json.ModelFactory;
import avalanche.crash.ingestion.models.ThreadFrame;

public class ThreadFrameFactory implements ModelFactory<ThreadFrame> {

    private static ThreadFrameFactory sInstance = new ThreadFrameFactory();

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
