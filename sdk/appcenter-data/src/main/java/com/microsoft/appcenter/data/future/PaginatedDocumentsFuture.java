package com.microsoft.appcenter.data.future;

import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.PaginatedDocuments;

public class PaginatedDocumentsFuture<T> extends DefaultDataFuture<PaginatedDocuments<T>> {

    @Override
    protected PaginatedDocuments<T> createExceptionInstance(Exception e) {
        return new PaginatedDocuments<T>().setCurrentPage(new Page<T>(e));
    }
}
