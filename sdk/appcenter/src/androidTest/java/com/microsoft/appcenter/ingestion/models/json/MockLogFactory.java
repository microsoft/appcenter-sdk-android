package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.Log;

import java.util.ArrayList;
import java.util.List;

public class MockLogFactory implements LogFactory, ModelFactory<Log> {

    @Override
    public Log create() {
        return new MockLog();
    }

    @Override
    public List<Log> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
