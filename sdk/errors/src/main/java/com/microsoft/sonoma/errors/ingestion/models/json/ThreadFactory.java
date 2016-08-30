package com.microsoft.sonoma.errors.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.ModelFactory;
import com.microsoft.sonoma.errors.ingestion.models.Thread;

import java.util.ArrayList;
import java.util.List;

public class ThreadFactory implements ModelFactory<Thread> {

    private static final ThreadFactory sInstance = new ThreadFactory();

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
