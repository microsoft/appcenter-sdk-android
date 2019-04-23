/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

public class DocumentMetadata {

    private String mPartition;

    private String mDocumentId;

    private String mETag;

    public DocumentMetadata(String partition, String documentId, String eTag) {
        mPartition = partition;
        mDocumentId = documentId;
        mETag = eTag;
    }

    public String getPartition() {
        return mPartition;
    }

    public String getDocumentId() {
        return mDocumentId;
    }

    public String getETag() {
        return mETag;
    }
}
