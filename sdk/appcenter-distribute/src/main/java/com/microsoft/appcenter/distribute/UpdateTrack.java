/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Track to use for in-app updates.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        UpdateTrack.PUBLIC,
        UpdateTrack.PRIVATE
})
public @interface UpdateTrack {

    /**
     * Releases from the public group that don't require authentication.
     */
    int PUBLIC = 1;

    /**
     * Releases from private groups that require authentication, also contain public releases.
     */
    int PRIVATE = 2;
}
