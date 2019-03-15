/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models.json;

import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.ingestion.models.json.ModelFactory;

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
