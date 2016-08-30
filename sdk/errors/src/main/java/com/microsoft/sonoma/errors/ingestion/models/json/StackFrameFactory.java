package com.microsoft.sonoma.errors.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.ModelFactory;
import com.microsoft.sonoma.errors.ingestion.models.StackFrame;

import java.util.ArrayList;
import java.util.List;

public class StackFrameFactory implements ModelFactory<StackFrame> {

    private static final StackFrameFactory sInstance = new StackFrameFactory();

    private StackFrameFactory() {
    }

    public static StackFrameFactory getInstance() {
        return sInstance;
    }

    @Override
    public StackFrame create() {
        return new StackFrame();
    }

    @Override
    public List<StackFrame> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
