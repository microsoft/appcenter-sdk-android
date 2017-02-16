package com.microsoft.azure.mobile.updates;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.HashSet;
import java.util.Set;

/**
 * Installer utils.
 */
class InstallerUtils {

    /**
     * Installer package names that are not app stores.
     */
    private static final Set<String> LOCAL_STORES = new HashSet<>();

    /**
     * Used to cache the result of {@link #isInstalledFromAppStore(String, Context)}, null until first call.
     */
    private static Boolean sInstalledFromAppStore;

    /* Populate local stores. */
    static {
        LOCAL_STORES.add("adb");
        LOCAL_STORES.add("com.google.android.packageinstaller");
    }

    @VisibleForTesting
    InstallerUtils() {

        /* Hide constructor in utils pattern. */
    }

    /**
     * Check if this installation was made via an application store.
     *
     * @param logTag  log tag for debug.
     * @param context any context.
     * @return true if the application was installed from an app store, false if it was installed via adb or via unknown source.
     */
    static synchronized boolean isInstalledFromAppStore(@NonNull String logTag, @NonNull Context context) {
        if (sInstalledFromAppStore == null) {
            String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
            MobileCenterLog.debug(logTag, "InstallerPackageName=" + installer);
            sInstalledFromAppStore = installer != null && !LOCAL_STORES.contains(installer);
        }
        return sInstalledFromAppStore;
    }
}
