package com.microsoft.azure.mobile.sasquatch.activities;

import android.support.annotation.Nullable;
import android.util.Log;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.ResultCallback;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.push.Push;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.mobile.sasquatch.activities.MainActivity.LOG_TAG;

public class GetHelper {

    static void testInstallIdAndLastSessionCrash() {

        /* Print install ID. */
        Log.i(LOG_TAG, "InstallId=" + MobileCenter.getInstallId());

        /* Print last crash. */
        Log.i(LOG_TAG, "Crashes.hasCrashedInLastSession=" + Crashes.hasCrashedInLastSession());
        Crashes.getLastSessionCrashReport(new ResultCallback<ErrorReport>() {

            @Override
            public void onResult(@Nullable ErrorReport data) {
                if (data != null) {
                    Log.i(LOG_TAG, "Crashes.getLastSessionCrashReport().getThrowable()=", data.getThrowable());
                }
            }
        });
    }

    static boolean hasCrashedInLastSession() {
        return Crashes.hasCrashedInLastSession();
    }

    static ErrorReport getLastSessionCrashReport() {
        final Semaphore semaphore = new Semaphore(0);
        final AtomicReference<ErrorReport> errorReport = new AtomicReference<>();
        Crashes.getLastSessionCrashReport(new ResultCallback<ErrorReport>() {

            @Override
            public void onResult(@Nullable ErrorReport data) {
                errorReport.set(data);
                semaphore.release();
            }
        });
        semaphore.acquireUninterruptibly();
        return errorReport.get();
    }

    static boolean isMobileCenterEnabled() {
        return MobileCenter.isEnabled();
    }

    static boolean isAnalyticsEnabled() {
        return Analytics.isEnabled();
    }

    static boolean isCrashesEnabled() {
        return Crashes.isEnabled();
    }

    static boolean isDistributeEnabled() {
        return Distribute.isEnabled();
    }

    static boolean isPushEnabled() {
        return Push.isEnabled();
    }
}
