package com.microsoft.azure.mobile.distribute;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
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
     * Value when {@link Settings.Secure#INSTALL_NON_MARKET_APPS} setting is enabled.
     */
    @VisibleForTesting
    static final String INSTALL_NON_MARKET_APPS_ENABLED = "1";

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
        LOCAL_STORES.add("com.android.packageinstaller");
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

    /**
     * Check whether user enabled installation via unknown sources.
     *
     * @param context any context.
     * @return true if installation via unknown sources is enabled, false otherwise.
     */
    @SuppressWarnings("deprecation")
    static boolean isUnknownSourcesEnabled(@NonNull Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return INSTALL_NON_MARKET_APPS_ENABLED.equals(Settings.Global.getString(contentResolver, Settings.Global.INSTALL_NON_MARKET_APPS));
        } else {
            return INSTALL_NON_MARKET_APPS_ENABLED.equals(Settings.Secure.getString(contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS));
        }
    }
}
