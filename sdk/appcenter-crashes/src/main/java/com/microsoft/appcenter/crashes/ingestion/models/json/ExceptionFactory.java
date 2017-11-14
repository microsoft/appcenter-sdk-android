package com.microsoft.appcenter.crashes.ingestion.models.json;

import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.ingestion.models.json.ModelFactory;

import java.util.ArrayList;
import java.util.List;

public class ExceptionFactory implements ModelFactory<Exception> {

    private static final ExceptionFactory sInstance = new ExceptionFactory();

    private ExceptionFactory() {
    }

    public static ExceptionFactory getInstance() {
        return sInstance;
    }

    @Override
    public Exception create() {
        return new Exception();
    }

    @Override
    public List<Exception> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
