package com.microsoft.azure.mobile;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Handler abstraction to share between core and services.
 */
public interface MobileCenterHandler {

    /**
     * Post a command to run on Mobile Center background event loop.
     *
     * @param runnable         command to run, not run if core not configured or disabled.
     * @param disabledRunnable optional alternate command to run if core is disabled.
     */
    void post(@NonNull Runnable runnable, @Nullable Runnable disabledRunnable);
}
