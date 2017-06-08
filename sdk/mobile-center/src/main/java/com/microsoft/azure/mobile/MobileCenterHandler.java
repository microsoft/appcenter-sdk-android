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
     * @param runnable         command to run if MobileCenter is enabled.
     * @param disabledRunnable optional alternate command to run if MobileCenter is disabled.
     */
    void post(@NonNull Runnable runnable, @Nullable Runnable disabledRunnable);
}
