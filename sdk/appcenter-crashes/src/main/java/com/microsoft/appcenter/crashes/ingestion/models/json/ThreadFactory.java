/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models.json;

import com.microsoft.appcenter.crashes.ingestion.models.Thread;
import com.microsoft.appcenter.ingestion.models.json.ModelFactory;

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
