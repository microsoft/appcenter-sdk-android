package com.microsoft.azure.mobile.utils;

import android.os.Process;

/**
 * Shutdown helper.
 */
public class ShutdownHelper {

    public static void shutdown() {
        shutdown(1);
    }

    public static void shutdown(int status) {
        Process.killProcess(Process.myPid());
        System.exit(status);
    }
}
