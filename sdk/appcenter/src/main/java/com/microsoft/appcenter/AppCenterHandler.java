/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Handler abstraction to share between core and services.
 */
public interface AppCenterHandler {

    /**
     * Post a command to run on App Center background event loop.
     *
     * @param runnable         command to run if App Center is enabled.
     * @param disabledRunnable optional alternate command to run if App Center is disabled.
     */
    void post(@NonNull Runnable runnable, @Nullable Runnable disabledRunnable);
}
