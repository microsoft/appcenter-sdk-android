package com.microsoft.azure.mobile.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * Utilities related to Handler class.
 */
public class HandlerUtils {

    /**
     * Main/UI thread Handler.
     */
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    /**
     * Runs the specified runnable on the UI thread.
     *
     * @param runnable the runnable to run on the UI thread.
     */
    public static void runOnUiThread(Runnable runnable) {
        if (Thread.currentThread() == sHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            sHandler.post(runnable);
        }
    }
}
