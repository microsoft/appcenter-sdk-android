package com.microsoft.sonoma.errors.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.ModelFactory;
import com.microsoft.sonoma.errors.ingestion.models.JavaException;

import java.util.ArrayList;
import java.util.List;

public class JavaExceptionFactory implements ModelFactory<JavaException> {

    private static final JavaExceptionFactory sInstance = new JavaExceptionFactory();

    private JavaExceptionFactory() {
    }

    public static JavaExceptionFactory getInstance() {
        return sInstance;
    }

    @Override
    public JavaException create() {
        return new JavaException();
    }

    @Override
    public List<JavaException> createList(int capacity) {
        return new ArrayList<>(capacity);
    }
}
