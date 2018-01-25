package com.microsoft.appcenter.crashes;

import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.util.Map;

public final class CrashesPrivateHelper {

    private CrashesPrivateHelper() {
    }

    public static void trackException(Throwable throwable, Map<String, String> properties) {
        Crashes.trackException(throwable, properties);
    }

    public static void saveUncaughtException(Thread thread, Throwable exception) {
        Crashes.getInstance().saveUncaughtException(thread, exception);
    }

    public static AppCenterFuture<String> getMinidumpDirectory() {
        return Crashes.getMinidumpDirectory();
    }
}
