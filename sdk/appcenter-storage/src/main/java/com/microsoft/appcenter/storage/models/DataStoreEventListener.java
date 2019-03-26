package com.microsoft.appcenter.storage.models;

public interface DataStoreEventListener {
    void onDataStoreOperationResult(String operation, DocumentMetadata document, DocumentError error);

}
