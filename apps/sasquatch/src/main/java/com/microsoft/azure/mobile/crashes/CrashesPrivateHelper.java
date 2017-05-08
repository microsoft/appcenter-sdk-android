package com.microsoft.azure.mobile.crashes;

import com.microsoft.azure.mobile.crashes.model.ErrorReport;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void trackException(Throwable throwable) {
        Crashes.trackException(throwable);
    }

    public static void saveUncaughtException(Thread thread, Throwable exception) {
        Crashes.getInstance().saveUncaughtException(thread, exception);
    }

    public static ErrorReport getLastSessionCrashReport() {
        return Crashes.getLastSessionCrashReport();
    }
}
