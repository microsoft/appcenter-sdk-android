/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.crashes.ingestion.models.json;

import com.microsoft.azure.mobile.crashes.ingestion.models.Thread;
import com.microsoft.azure.mobile.ingestion.models.json.ModelFactory;

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
