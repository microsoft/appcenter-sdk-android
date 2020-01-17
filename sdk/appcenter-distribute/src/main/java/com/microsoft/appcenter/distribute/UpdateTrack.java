/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

/**
 * Track to use for in-app updates.
 */
public enum UpdateTrack {

    /**
     * Releases from the public group that don't require authentication.
     */
    PUBLIC,

    /**
     * Releases from private groups that require authentication, also contain public releases.
     */
    PRIVATE
}
