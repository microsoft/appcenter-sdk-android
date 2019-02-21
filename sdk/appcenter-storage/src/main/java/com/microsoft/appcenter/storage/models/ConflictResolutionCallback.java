package com.microsoft.appcenter.storage.models;

public interface ConflictResolutionCallback<T> {

    /**
     * Resolve server side conflicts
     * @param localDocument the local document that the server rejected
     * @param remoteDocument the remote document that the server currently tracks
     * @return
     * - null, to abort the operation
     * - a new document to submit to the server (and override the existing remote document)
     */
    T resolve(Document<T> localDocument, Document<T> remoteDocument);
}