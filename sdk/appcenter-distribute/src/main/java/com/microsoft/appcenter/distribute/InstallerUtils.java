/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Installer utils.
 */
public class InstallerUtils {

    /**
     * Value when {@link Settings.Secure#INSTALL_NON_MARKET_APPS} setting is enabled.
     */
    @VisibleForTesting
    static final String INSTALL_NON_MARKET_APPS_ENABLED = "1";

    /**
     * Name of package installer stream.
     */
    private static final String sOutputStreamName = "AppCenterPackageInstallerStream";

    /**
     * Buffer capacity of package installer.
     */
    private static final int sBufferCapacity = 16384;

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
        LOCAL_STORES.add("com.android.managedprovisioning");
        LOCAL_STORES.add("com.miui.packageinstaller");
        LOCAL_STORES.add("com.samsung.android.packageinstaller");
        LOCAL_STORES.add("pc");
        LOCAL_STORES.add("com.google.android.apps.nbu.files");
        LOCAL_STORES.add("org.mozilla.firefox");
        LOCAL_STORES.add("com.android.chrome");
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
            AppCenterLog.debug(logTag, "InstallerPackageName=" + installer);
            sInstalledFromAppStore = installer != null && !LOCAL_STORES.contains(installer);
        }
        return sInstalledFromAppStore;
    }

    /**
     * Add new stores to local stores list.
     *
     * @param stores list of stores.
     */
    public static void addLocalStores(Set<String> stores) {
        LOCAL_STORES.addAll(stores);
    }

    /**
     * Check whether user enabled installation via unknown sources.
     *
     * @param context any context.
     * @return true if installation via unknown sources is enabled, false otherwise.
     */
    @SuppressWarnings("deprecation")
    public static boolean isUnknownSourcesEnabled(@NonNull Context context) {

        /*
         * On Android 8 with applications targeting lower versions,
         * it's impossible to check unknown sources enabled: using old APIs will always return true
         * and using the new one will always return false,
         * so in order to avoid a stuck dialog that can't be bypassed we will assume true.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.O || context.getPackageManager().canRequestPackageInstalls();
        } else {
            return INSTALL_NON_MARKET_APPS_ENABLED.equals(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS));
        }
    }

    /**
     * Check whether user enabled start activity from the background.
     *
     * @param context any context.
     * @return true if start activity from the background is enabled, false otherwise.
     */
    public synchronized static boolean isSystemAlertWindowsEnabled(@NonNull Context context) {

        /*
        * From Android 10 (29 API level) or higher we have to
        * use this permission for restarting activity after update.
        * See more about restrictions on starting activities from the background:
        * - https://developer.android.com/guide/components/activities/background-starts
        * - https://developer.android.com/about/versions/10/behavior-changes-all#sysalert-go
        */
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                Settings.canDrawOverlays(context);
    }

    /**
     * Install a new release.
     *
     * @param data input stream data from the installing apk file.
     */
    public synchronized static void installPackage(@NonNull InputStream data, Context context, PackageInstaller.SessionCallback sessionCallback) {
        PackageInstaller.Session session = null;
        OutputStream out;
        try {

            /* Prepare package installer. */
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            if (sessionCallback != null) {
                packageInstaller.registerSessionCallback(sessionCallback);
            }
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);

            /* Prepare session. */
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);

            /* Start to install a new release. */
            out = session.openWrite(sOutputStreamName, 0, -1);
            byte[] buffer = new byte[sBufferCapacity];
            int c;
            while ((c = data.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            data.close();
            out.close();
            session.commit(createIntentSender(context, sessionId));
        } catch (IOException e) {
            if (session != null) {
                session.abandon();
            }
            AppCenterLog.error(LOG_TAG, "Couldn't install a new release.", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Return IntentSender with the receiver that listens to the package installer session status.
     *
     * @param context any context.
     * @param sessionId install sessionId.
     * @return IntentSender with receiver.
     */
    public synchronized static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(AppCenterPackageInstallerReceiver.START_ACTION),
                0);
        return pendingIntent.getIntentSender();
    }
}
