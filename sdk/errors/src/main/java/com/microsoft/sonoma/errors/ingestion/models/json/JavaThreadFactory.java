package com.microsoft.sonoma.errors.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.ModelFactory;
import com.microsoft.sonoma.errors.ingestion.models.JavaThread;

import java.util.ArrayList;
import java.util.List;

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
