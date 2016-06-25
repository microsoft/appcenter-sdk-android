package avalanche.crash.ingestion.models.json;

import java.util.ArrayList;
import java.util.List;

import avalanche.base.ingestion.models.json.DefinitionFactory;
import avalanche.crash.ingestion.models.Thread;

public class ThreadFactory implements DefinitionFactory<Thread> {

    private static ThreadFactory sInstance = new ThreadFactory();

    private ThreadFactory() {
    }

    public static ThreadFactory getInstance() {
        return sInstance;
    }

    @Override
    public Thread create() {
        return new Thread();
    }

    @Override
    public List<Thread> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
