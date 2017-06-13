package com.microsoft.azure.mobile.sasquatch.activities;

import android.util.Log;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.push.Push;
import com.microsoft.azure.mobile.utils.async.MobileCenterConsumer;

import java.util.UUID;

import static com.microsoft.azure.mobile.sasquatch.activities.MainActivity.LOG_TAG;

class GetHelper {

    static void testInstallIdAndLastSessionCrash() {

        /* Print install ID. */
        MobileCenter.getInstallId().thenAccept(new MobileCenterConsumer<UUID>() {

            @Override
            public void accept(UUID uuid) {
                Log.i(LOG_TAG, "InstallId=" + uuid);
            }
        });

        /* Print last crash. */
        Crashes.hasCrashedInLastSession().thenAccept(new MobileCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean crashed) {
                Log.i(LOG_TAG, "Crashes.hasCrashedInLastSession=" + crashed);
            }
        });
        Crashes.getLastSessionCrashReport().thenAccept(new MobileCenterConsumer<ErrorReport>() {

            @Override
            public void accept(ErrorReport data) {
                if (data != null) {
                    Log.i(LOG_TAG, "Crashes.getLastSessionCrashReport().getThrowable()=", data.getThrowable());
                }
            }
        });
    }

    static boolean hasCrashedInLastSession() {
        return Crashes.hasCrashedInLastSession().get();
    }

    static ErrorReport getLastSessionCrashReport() {
        return Crashes.getLastSessionCrashReport().get();
    }

    static boolean isMobileCenterEnabled() {
        return MobileCenter.isEnabled().get();
    }

    static boolean isAnalyticsEnabled() {
        return Analytics.isEnabled().get();
    }

    static boolean isCrashesEnabled() {
        return Crashes.isEnabled().get();
    }

    static boolean isDistributeEnabled() {
        return Distribute.isEnabled().get();
    }

    static boolean isPushEnabled() {
        return Push.isEnabled().get();
    }
}
