/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.data.Constants;

public class DocumentMetadata {

    @SerializedName(value = Constants.ETAG_FIELD_NAME)
    String mETag;

    @SerializedName(value = Constants.PARTITION_KEY_FIELD_NAME)
    private String mPartition;

    @SerializedName(value = Constants.ID_FIELD_NAME)
    private String mId;

    DocumentMetadata() {
    }

    public DocumentMetadata(String partition, String Id, String eTag) {
        mPartition = partition;
        mId = Id;
        mETag = eTag;
    }

    /**
     * Get document partition.
     *
     * @return Document partition.
     */
    public String getPartition() {
        return mPartition;
    }

    /**
     * Get document id.
     *
     * @return Document id.
     */
    public String getId() {
        return mId;
    }

    /**
     * Get ETag.
     *
     * @return ETag.
     */
    public String getETag() {
        return mETag;
    }
}
