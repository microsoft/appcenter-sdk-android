/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

/**
 * Constants defining default partitions.
 */
@SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
public final class DefaultPartitions {

    /**
     * User partition.
     * An authenticated user can read/write documents in this partition.
     */
    public static final String USER_DOCUMENTS = "user";

    /**
     * Readonly partition.
     * Everyone can read documents in this partition.
     * Writes are not allowed via the SDK.
     */
    public static final String APP_DOCUMENTS = "readonly";
}
