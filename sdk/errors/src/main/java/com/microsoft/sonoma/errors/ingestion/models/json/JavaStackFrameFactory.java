package com.microsoft.sonoma.errors.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.ModelFactory;
import com.microsoft.sonoma.errors.ingestion.models.JavaStackFrame;

import java.util.ArrayList;
import java.util.List;

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
