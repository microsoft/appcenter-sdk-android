package com.microsoft.appcenter.crashes;

import java.util.Map;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void trackException(Throwable throwable, Map<String, String> properties) {
        try {
            Crashes.class.getMethod("trackException", Throwable.class, Map.class).invoke(null, throwable, properties);
        } catch (Exception ignored) {
        }
    }

    public static void saveUncaughtException(Thread thread, Throwable exception) {
        Crashes.getInstance().saveUncaughtException(thread, exception);
    }
}
