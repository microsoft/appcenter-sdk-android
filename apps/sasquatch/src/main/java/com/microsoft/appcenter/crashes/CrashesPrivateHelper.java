package com.microsoft.appcenter.crashes;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void trackException(Throwable throwable) {
        Crashes.trackException(throwable);
    }

    public static void saveUncaughtException(Thread thread, Throwable exception) {
        Crashes.getInstance().saveUncaughtException(thread, exception);
    }
}
