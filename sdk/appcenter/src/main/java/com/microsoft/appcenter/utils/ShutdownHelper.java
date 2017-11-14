package com.microsoft.appcenter.utils;

import android.os.Process;
import android.support.annotation.VisibleForTesting;

/**
 * Shutdown helper.
 */
public class ShutdownHelper {

    @VisibleForTesting
    ShutdownHelper() {
        /* Hide constructor. */
    }

    public static void shutdown(int status) {
        Process.killProcess(Process.myPid());
        System.exit(status);
    }
}
