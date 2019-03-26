package com.microsoft.appcenter.storage.models;

public class DocumentMetadata {
    private String partition;
    private String documentId;
    private String etag;

    public DocumentMetadata(String partition, String documentId, String etag) {
        this.partition = partition;
        this.documentId = documentId;
        this.etag = etag;
    }

    public String getPartition() {
        return partition;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getEtag() {
        return etag;
    }
}
