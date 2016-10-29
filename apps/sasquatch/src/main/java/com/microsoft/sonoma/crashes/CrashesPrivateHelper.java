package com.microsoft.sonoma.crashes;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void trackException(Throwable throwable) {
        Crashes.trackException(throwable);
    }
}
