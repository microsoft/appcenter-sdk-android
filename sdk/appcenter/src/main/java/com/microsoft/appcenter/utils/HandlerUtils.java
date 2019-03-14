/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;

/**
 * Utilities related to Handler class.
 */
public class HandlerUtils {

    /**
     * Main/UI thread Handler.
     */
    @VisibleForTesting
    static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    /**
     * Runs the specified runnable on the UI thread.
     *
     * @param runnable the runnable to run on the UI thread.
     */
    public static void runOnUiThread(Runnable runnable) {
        if (Thread.currentThread() == sMainHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            sMainHandler.post(runnable);
        }
    }

    /**
     * Main thread handler.
     *
     * @return main thread handler.
     */
    public static Handler getMainHandler() {
        return sMainHandler;
    }
}
